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
	"crypto/x509"
	"net"
	"net/url"
	"strconv"
	"strings"

	"github.com/apache/seata/tools/seata-ai-cli/internal/protocol"
)

type EndpointConfig struct {
	URL         string
	Environment string
	Accepted    bool
	RootCAs     *x509.CertPool
}

// NormalizeEndpoint retains the management base path. Credentials, URL tokens,
// fragments and ambiguous separators cannot become part of an accepted target.
func NormalizeEndpoint(raw, environment string) (*url.URL, error) {
	invalid := func() (*url.URL, error) {
		return nil, protocol.Failure("validation", "invalid_endpoint", "Endpoint is not an unambiguous HTTP(S) management base URL.")
	}
	if len(raw) == 0 || len(raw) > 2048 || strings.ContainsAny(raw, "\\\r\n\t") {
		return invalid()
	}
	if environment != "production" && environment != "staging" && environment != "development" {
		return invalid()
	}
	u, err := url.Parse(raw)
	if err != nil || u.Opaque != "" || u.Host == "" || u.User != nil || u.RawQuery != "" || u.ForceQuery || u.Fragment != "" || strings.Contains(raw, "#") {
		return invalid()
	}
	u.Scheme = strings.ToLower(u.Scheme)
	if u.Scheme != "https" && u.Scheme != "http" {
		return invalid()
	}
	if strings.Contains(u.Path, "//") {
		return invalid()
	}
	escaped := strings.ToLower(u.EscapedPath())
	if strings.Contains(escaped, "%2f") || strings.Contains(escaped, "%5c") {
		return invalid()
	}
	for _, segment := range strings.Split(u.Path, "/") {
		if segment == "." || segment == ".." {
			return invalid()
		}
	}
	host := strings.ToLower(u.Hostname())
	if host == "" {
		return invalid()
	}
	if u.Scheme == "http" {
		ip := net.ParseIP(host)
		// A literal loopback address avoids a DNS change turning development HTTP
		// into a credential-bearing connection to an external host.
		if environment != "development" || ip == nil || !ip.IsLoopback() {
			return invalid()
		}
	}
	port := u.Port()
	if port != "" {
		number, err := strconv.Atoi(port)
		if err != nil || number < 1 || number > 65535 {
			return invalid()
		}
		port = strconv.Itoa(number)
	} else if strings.HasSuffix(u.Host, ":") {
		return invalid()
	}
	if (u.Scheme == "https" && port == "443") || (u.Scheme == "http" && port == "80") {
		port = ""
	}
	if port != "" {
		u.Host = net.JoinHostPort(host, port)
	} else {
		u.Host = host
		if strings.Contains(host, ":") {
			u.Host = "[" + host + "]"
		}
	}
	u.Path = strings.TrimSuffix(u.Path, "/")
	u.RawPath = strings.TrimSuffix(u.RawPath, "/")
	return u, nil
}
