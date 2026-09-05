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
	"crypto/tls"
	"errors"
	"io"
	"math/rand/v2"
	"net"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
)

// These GETs are source-inspected reads at the pinned Seata baseline. There is
// deliberately no arbitrary method/path entry point or direct-TC discovery.
type Operation string

const (
	NamespaceQuery    Operation = "/api/v1/naming/namespace"
	GlobalQuery       Operation = "/api/v1/console/globalSession/query"
	LockQuery         Operation = "/api/v1/console/globalLock/query"
	OwnerSessionCheck Operation = "/api/v1/console/globalLock/check"
	MaxReadResponse             = 4 << 20
)

func (op Operation) valid() bool {
	switch op {
	case NamespaceQuery, GlobalQuery, LockQuery, OwnerSessionCheck:
		return true
	}
	return false
}

type ReadClient struct {
	base   *url.URL
	client *http.Client
}
type ReadResponse struct {
	Value      any
	Attempts   int
	ObservedAt time.Time
}

func NewReadClient(config EndpointConfig) (*ReadClient, error) {
	if !config.Accepted {
		return nil, protocol.Failure("policy", "trust_required", "The management target must be explicitly accepted.")
	}
	base, err := NormalizeEndpoint(config.URL, config.Environment)
	if err != nil {
		return nil, err
	}
	t := &http.Transport{
		Proxy: nil, DialContext: (&net.Dialer{Timeout: 10 * time.Second, KeepAlive: -1}).DialContext,
		DisableKeepAlives: true, DisableCompression: true, ForceAttemptHTTP2: false,
		TLSNextProto:        map[string]func(string, *tls.Conn) http.RoundTripper{},
		TLSClientConfig:     &tls.Config{RootCAs: config.RootCAs, MinVersion: tls.VersionTLS12},
		TLSHandshakeTimeout: 10 * time.Second, ResponseHeaderTimeout: 15 * time.Second, MaxResponseHeaderBytes: 16 << 10,
	}
	return &ReadClient{base: base, client: &http.Client{Transport: t, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}}, nil
}
func (c *ReadClient) Read(ctx context.Context, op Operation, query url.Values, headers http.Header) (ReadResponse, error) {
	if !op.valid() {
		return ReadResponse{}, protocol.Failure("policy", "command_unavailable", "The endpoint is not an approved source-inspected read operation.")
	}
	if _, hasDeadline := ctx.Deadline(); !hasDeadline {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, 30*time.Second)
		defer cancel()
	}
	header, err := readHeaders(headers)
	if err != nil {
		return ReadResponse{}, err
	}
	target := *c.base
	target.Path += string(op)
	if target.RawPath != "" {
		target.RawPath += string(op)
	}
	target.RawQuery = query.Encode()
	var result ReadResponse
	for attempt := 1; attempt <= 3; attempt++ {
		if ctx.Err() != nil {
			return result, readFailure(ctx.Err())
		}
		request, err := http.NewRequestWithContext(ctx, http.MethodGet, target.String(), nil)
		if err != nil {
			return result, protocol.Failure("validation", "invalid_argument", "Cannot construct the read request.")
		}
		request.Header = header.Clone()
		request.Header.Set("Accept", "application/json")
		request.Header.Set("Accept-Encoding", "identity")
		result.Attempts = attempt
		response, err := c.client.Do(request)
		if err != nil {
			if response != nil && response.Body != nil {
				response.Body.Close()
			}
			if attempt < 3 && transient(err) && ctx.Err() == nil {
				if e := waitRead(ctx, backoff(attempt)); e == nil {
					continue
				} else {
					return result, e
				}
			}
			return result, readFailure(err)
		}
		result.ObservedAt = time.Now().UTC()
		status := response.StatusCode
		if status >= 200 && status < 300 {
			value, e := decodeReadBody(response)
			response.Body.Close()
			if ctx.Err() != nil {
				return result, readFailure(ctx.Err())
			}
			if e != nil {
				return result, e
			}
			result.Value = value
			return result, nil
		}
		// No raw failure body is retained or interpolated into public errors.
		delay := retryDelay(response, attempt)
		response.Body.Close()
		if attempt < 3 && retryStatus(status) {
			if e := waitRead(ctx, delay); e != nil {
				return result, e
			}
			continue
		}
		switch {
		case status == 401:
			return result, protocol.Failure("authentication", "authentication_required", "The management endpoint rejected authentication.")
		case status == 403:
			return result, protocol.Failure("authorization", "access_denied", "The management endpoint denied access.")
		case status >= 300 && status < 400:
			return result, protocol.Failure("network", "redirect_denied", "Management reads do not follow redirects.")
		default:
			f := protocol.Failure("api", "remote_error", "The management endpoint returned an unsuccessful HTTP status.")
			f.Details["http_status"] = status
			if retryStatus(status) {
				f.Retry = "read_only"
			}
			return result, f
		}
	}
	return result, protocol.Failure("internal", "unexpected", "Read attempt budget exhausted.")
}
func readHeaders(input http.Header) (http.Header, error) {
	out := http.Header{}
	for key, values := range input {
		key = http.CanonicalHeaderKey(key)
		switch key {
		case "Authorization", "X-Seata-Namespace", "X-Seata-Cluster":
		default:
			return nil, protocol.Failure("validation", "invalid_argument", "Unsupported management header.")
		}
		if len(values) != 1 || len(values[0]) > 8192 || strings.ContainsAny(values[0], "\r\n\x00") {
			return nil, protocol.Failure("validation", "invalid_argument", "Invalid management header.")
		}
		if _, duplicate := out[key]; duplicate {
			return nil, protocol.Failure("validation", "invalid_argument", "Duplicate management header.")
		}
		out.Set(key, values[0])
	}
	return out, nil
}
func decodeReadBody(r *http.Response) (any, error) {
	if encoding := r.Header.Get("Content-Encoding"); encoding != "" && encoding != "identity" {
		return nil, protocol.Failure("network", "protocol", "The management endpoint ignored identity response encoding.")
	}
	if r.ContentLength > MaxReadResponse {
		return nil, protocol.Failure("api", "response_too_large", "Read response exceeds the configured byte limit.")
	}
	v, err := protocol.DecodeLimit(r.Body, MaxReadResponse)
	if err != nil {
		kind := "decode_failure"
		if err.Error() == "input_too_large" {
			kind = "response_too_large"
		}
		return nil, protocol.Failure("api", kind, "The management response is oversized or malformed JSON.")
	}
	return v, nil
}
func retryStatus(status int) bool {
	return status == 429 || status == 502 || status == 503 || status == 504
}
func backoff(attempt int) time.Duration {
	base := 100 * time.Millisecond
	if attempt > 1 {
		base = 300 * time.Millisecond
	}
	return base + time.Duration(rand.IntN(21))*time.Millisecond
}
func retryDelay(response *http.Response, attempt int) time.Duration {
	delay := backoff(attempt)
	value := response.Header.Get("Retry-After")
	if seconds, err := strconv.ParseInt(value, 10, 32); err == nil && seconds > 0 {
		if seconds > 1 {
			return time.Second
		}
		return time.Duration(seconds) * time.Second
	}
	if timestamp, err := http.ParseTime(value); err == nil {
		d := time.Until(timestamp)
		if d > time.Second {
			return time.Second
		}
		if d > delay {
			return d
		}
	}
	return delay
}
func waitRead(ctx context.Context, d time.Duration) error {
	timer := time.NewTimer(d)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return readFailure(ctx.Err())
	case <-timer.C:
		return nil
	}
}
func transient(err error) bool {
	if errors.Is(err, io.EOF) || errors.Is(err, io.ErrUnexpectedEOF) {
		return true
	}
	var dns *net.DNSError
	if errors.As(err, &dns) && dns.IsNotFound {
		return false
	}
	var op *net.OpError
	if errors.As(err, &op) {
		return op.Op == "dial" || op.Op == "read"
	}
	return false
}
func readFailure(err error) *protocol.Fault {
	if errors.Is(err, context.Canceled) {
		return protocol.Failure("cancelled", "context_cancelled", "The read invocation was cancelled.")
	}
	f := protocol.Failure("network", "connection_failed", "The management read could not complete.")
	var network net.Error
	if errors.Is(err, context.DeadlineExceeded) || (errors.As(err, &network) && network.Timeout()) {
		f.Subtype = "timeout"
	}
	f.Retry = "read_only"
	return f
}
