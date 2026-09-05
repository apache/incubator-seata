/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package transport

import (
	"context"
	"crypto/x509"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strconv"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
)

func TestTrustedEndpointAndBasePath(t *testing.T) {
	var hits atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		hits.Add(1)
		if r.URL.Path != "/seata/api/v1/console/globalSession/query" || r.URL.Query().Get("xid") != "a:b/123" || r.URL.Query().Get("vGroup") != "group" {
			t.Errorf("wrong routing: %s", r.URL)
		}
		if r.Header.Get("Authorization") != "Bearer synthetic" || r.Header.Get("x-seata-namespace") != "public" {
			t.Error("missing route or credential")
		}
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"data":[{"transactionId":9223372036854775807}]}`))
	}))
	defer server.Close()
	client, err := NewReadClient(EndpointConfig{URL: server.URL + "/seata", Environment: "development", Accepted: true})
	if err != nil {
		t.Fatal(err)
	}
	result, err := client.Read(context.Background(), GlobalQuery, url.Values{"xid": {"a:b/123"}, "vGroup": {"group"}}, http.Header{"Authorization": {"Bearer synthetic"}, "X-Seata-Namespace": {"public"}})
	if err != nil {
		t.Fatal(err)
	}
	if result.Attempts != 1 || hits.Load() != 1 {
		t.Fatal(result.Attempts, hits.Load())
	}
	if got := result.Value.(map[string]any)["data"].([]any)[0].(map[string]any)["transactionId"].(json.Number).String(); got != "9223372036854775807" {
		t.Fatal(got)
	}
}
func TestReadRedirectNeverForwardsCredential(t *testing.T) {
	for _, status := range []int{301, 302, 303, 307, 308} {
		t.Run(strconv.Itoa(status), func(t *testing.T) {
			var targets atomic.Int32
			target := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { targets.Add(1) }))
			defer target.Close()
			source := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				w.Header().Set("Location", target.URL)
				w.WriteHeader(status)
			}))
			defer source.Close()
			c, err := NewReadClient(EndpointConfig{URL: source.URL, Environment: "development", Accepted: true})
			if err != nil {
				t.Fatal(err)
			}
			_, err = c.Read(context.Background(), NamespaceQuery, nil, http.Header{"Authorization": {"Bearer secret-marker"}})
			if err == nil || strings.Contains(err.Error(), "secret-marker") || targets.Load() != 0 {
				t.Fatal(err, targets.Load())
			}
		})
	}
}
func TestReadRetryBoundAndCancellation(t *testing.T) {
	for _, status := range []int{400, 401, 403, 429, 500, 502, 503, 504} {
		t.Run(strconv.Itoa(status), func(t *testing.T) {
			var count atomic.Int32
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				count.Add(1)
				w.WriteHeader(status)
				_, _ = w.Write([]byte("secret-marker"))
			}))
			defer server.Close()
			c, err := NewReadClient(EndpointConfig{URL: server.URL, Environment: "development", Accepted: true})
			if err != nil {
				t.Fatal(err)
			}
			_, err = c.Read(context.Background(), NamespaceQuery, nil, nil)
			if err == nil || strings.Contains(err.Error(), "secret-marker") {
				t.Fatal(err)
			}
			expected := int32(1)
			if status == 429 || status == 502 || status == 503 || status == 504 {
				expected = 3
			}
			if count.Load() != expected {
				t.Fatal(count.Load(), expected)
			}
		})
	}
	var count atomic.Int32
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		count.Add(1)
		w.Header().Set("Retry-After", "3600")
		w.WriteHeader(503)
	}))
	defer server.Close()
	c, _ := NewReadClient(EndpointConfig{URL: server.URL, Environment: "development", Accepted: true})
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Millisecond)
	defer cancel()
	start := time.Now()
	_, err := c.Read(ctx, NamespaceQuery, nil, nil)
	var fault *protocol.Fault
	if !errors.As(err, &fault) || fault.Subtype != "timeout" || time.Since(start) > time.Second || count.Load() != 1 {
		t.Fatal(err, count.Load())
	}
}
func TestUnsafeEndpointsAndUnapprovedOperations(t *testing.T) {
	for _, endpoint := range []string{"http://example.com", "https://user:password@example.com", "https://example.com?token=x", "https://example.com#fragment", "https://example.com/a/../b", "https://example.com/a%2fb", "https://example.com/a%5cb", "https://example.com/%2e%2e/b", "ftp://example.com", "https://example.com:0", "https://example.com:65536", "https://example.com:"} {
		if _, err := NewReadClient(EndpointConfig{URL: endpoint, Environment: "development", Accepted: true}); err == nil {
			t.Fatal(endpoint)
		}
	}
	if _, err := NewReadClient(EndpointConfig{URL: "https://example.com", Environment: "production"}); err == nil {
		t.Fatal("unaccepted target allowed")
	}
	c, err := NewReadClient(EndpointConfig{URL: "https://example.com/base", Environment: "production", Accepted: true})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = c.Read(context.Background(), Operation("/api/v1/console/globalSession/deleteGlobalSession"), nil, nil); err == nil {
		t.Fatal("write suffix admitted")
	}
}

func TestReadBodyLimitsAndEncoding(t *testing.T) {
	for _, tc := range []struct {
		name, body, encoding string
		wantError            bool
	}{
		{"larger_than_invocation_limit", `{"value":"` + strings.Repeat("a", 2<<20) + `"}`, "", false},
		{"too_large", `{"value":"` + strings.Repeat("a", MaxReadResponse) + `"}`, "", true},
		{"duplicate_key", `{"data":1,"data":2}`, "", true},
		{"trailing_json", `{} {}`, "", true},
		{"compressed", `{}`, "gzip", true},
	} {
		t.Run(tc.name, func(t *testing.T) {
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if r.Header.Get("Accept-Encoding") != "identity" {
					t.Error("compression not explicitly disabled")
				}
				if tc.encoding != "" {
					w.Header().Set("Content-Encoding", tc.encoding)
				}
				w.(http.Flusher).Flush()
				_, _ = w.Write([]byte(tc.body))
			}))
			defer server.Close()
			c, err := NewReadClient(EndpointConfig{URL: server.URL, Environment: "development", Accepted: true})
			if err != nil {
				t.Fatal(err)
			}
			_, err = c.Read(context.Background(), NamespaceQuery, nil, nil)
			if (err != nil) != tc.wantError {
				t.Fatal(err)
			}
		})
	}
}
func TestReadIgnoresEnvironmentProxyAndRetainsTLSVerification(t *testing.T) {
	var proxyHits atomic.Int32
	proxy := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { proxyHits.Add(1) }))
	defer proxy.Close()
	t.Setenv("HTTP_PROXY", proxy.URL)
	t.Setenv("HTTPS_PROXY", proxy.URL)
	server := httptest.NewTLSServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { _, _ = w.Write([]byte(`{}`)) }))
	defer server.Close()
	untrusted, err := NewReadClient(EndpointConfig{URL: server.URL, Environment: "production", Accepted: true})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = untrusted.Read(context.Background(), NamespaceQuery, nil, nil); err == nil {
		t.Fatal("untrusted server certificate accepted")
	}
	roots := x509.NewCertPool()
	roots.AddCert(server.Certificate())
	trusted, err := NewReadClient(EndpointConfig{URL: server.URL, Environment: "production", Accepted: true, RootCAs: roots})
	if err != nil {
		t.Fatal(err)
	}
	if _, err = trusted.Read(context.Background(), NamespaceQuery, nil, nil); err != nil {
		t.Fatal(err)
	}
	if proxyHits.Load() != 0 {
		t.Fatal("inherited environment proxy")
	}
}
func TestReadCancellationDuringBody(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.(http.Flusher).Flush()
		<-r.Context().Done()
	}))
	defer server.Close()
	c, err := NewReadClient(EndpointConfig{URL: server.URL, Environment: "development", Accepted: true})
	if err != nil {
		t.Fatal(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()
	_, err = c.Read(ctx, NamespaceQuery, nil, nil)
	var fault *protocol.Fault
	if !errors.As(err, &fault) || fault.Type != "network" || fault.Subtype != "timeout" {
		t.Fatal(err)
	}
}
