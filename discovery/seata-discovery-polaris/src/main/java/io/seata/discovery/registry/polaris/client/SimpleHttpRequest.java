/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.discovery.registry.polaris.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.Proxy.Type.HTTP;

/**
 * A fluid interface for making HTTP requests using an underlying {@link HttpURLConnection} (or sub-class).
 *
 * <p>Each instance supports making a single request and cannot be reused for further requests.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-17
 */
final class SimpleHttpRequest {

    /**
     * 'UTF-8' charset name
     */
    static final String CHARSET_UTF8 = "UTF-8";

    /**
     * 'application/x-www-form-urlencoded' content type header value
     */
    static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /**
     * 'application/json' content type header value
     */
    static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * 'gzip' encoding header value
     */
    static final String ENCODING_GZIP = "gzip";

    /**
     * 'Accept' header name
     */
    static final String HEADER_ACCEPT = "Accept";

    /**
     * 'Accept-Charset' header name
     */
    static final String HEADER_ACCEPT_CHARSET = "Accept-Charset";

    /**
     * 'Accept-Encoding' header name
     */
    static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";

    /**
     * 'Authorization' header name
     */
    static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * 'Cache-Control' header name
     */
    static final String HEADER_CACHE_CONTROL = "Cache-Control";

    /**
     * 'Content-Encoding' header name
     */
    static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

    /**
     * 'Content-Length' header name
     */
    static final String HEADER_CONTENT_LENGTH = "Content-Length";

    /**
     * 'Content-Type' header name
     */
    static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * 'Date' header name
     */
    static final String HEADER_DATE = "Date";

    /**
     * 'ETag' header name
     */
    static final String HEADER_ETAG = "ETag";

    /**
     * 'Expires' header name
     */
    static final String HEADER_EXPIRES = "Expires";

    /**
     * 'If-None-Match' header name
     */
    static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    /**
     * 'Last-Modified' header name
     */
    static final String HEADER_LAST_MODIFIED = "Last-Modified";

    /**
     * 'Location' header name
     */
    static final String HEADER_LOCATION = "Location";

    /**
     * 'Proxy-Authorization' header name
     */
    static final String HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization";

    /**
     * 'Referer' header name
     */
    static final String HEADER_REFERER = "Referer";

    /**
     * 'Server' header name
     */
    static final String HEADER_SERVER = "Server";

    /**
     * 'User-Agent' header name
     */
    static final String HEADER_USER_AGENT = "User-Agent";

    /**
     * 'DELETE' request method
     */
    static final String METHOD_DELETE = "DELETE";

    /**
     * 'GET' request method
     */
    static final String METHOD_GET = "GET";

    /**
     * 'HEAD' request method
     */
    static final String METHOD_HEAD = "HEAD";

    /**
     * 'OPTIONS' options method
     */
    static final String METHOD_OPTIONS = "OPTIONS";

    /**
     * 'POST' request method
     */
    static final String METHOD_POST = "POST";

    /**
     * 'PUT' request method
     */
    static final String METHOD_PUT = "PUT";

    /**
     * 'TRACE' request method
     */
    static final String METHOD_TRACE = "TRACE";

    /**
     * 'charset' header value parameter
     */
    static final String PARAM_CHARSET = "charset";

    private static final String BOUNDARY = "00content0boundary00";

    private static final String CONTENT_TYPE_MULTIPART = "multipart/form-data; boundary=" + BOUNDARY;

    private static final String CRLF = "\r\n";

    private static final String[] EMPTY_STRINGS = new String[0];
    private static ConnectionFactory CONNECTION_FACTORY = ConnectionFactory.DEFAULT;
    private final URL url;
    private final String requestMethod;
    private HttpURLConnection connection = null;
    private RequestOutputStream output;
    private boolean multipart;
    private boolean form;
    private boolean ignoreCloseExceptions = true;
    private boolean uncompress = false;
    private int bufferSize = 8192;
    private long totalSize = -1;
    private long totalWritten = 0;
    private String httpProxyHost;
    private int httpProxyPort;
    private UploadProgress progress = UploadProgress.DEFAULT;

    /**
     * Create HTTP connection wrapper
     *
     * @param url    Remote resource URL.
     * @param method HTTP request method (e.g., "GET", "POST").
     * @throws HttpRequestException
     */
    public SimpleHttpRequest(final CharSequence url, final String method) throws HttpRequestException {
        try {
            this.url = new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new HttpRequestException(e);
        }
        this.requestMethod = method;
    }

    /**
     * Create HTTP connection wrapper
     *
     * @param url    Remote resource URL.
     * @param method HTTP request method (e.g., "GET", "POST").
     * @throws HttpRequestException
     */
    public SimpleHttpRequest(final URL url, final String method) throws HttpRequestException {
        this.url = url;
        this.requestMethod = method;
    }

    private static String getValidCharset(final String charset) {
        if (charset != null && charset.length() > 0) {
            return charset;
        } else {
            return CHARSET_UTF8;
        }
    }
    private static StringBuilder addPathSeparator(final String baseUrl, final StringBuilder result) {
        // Add trailing slash if the base URL doesn't have any path segments.
        //
        // The following test is checking for the last slash not being part of
        // the protocol to host separator: '://'.
        if (baseUrl.indexOf(':') + 2 == baseUrl.lastIndexOf('/')) {
            result.append('/');
        }
        return result;
    }

    private static StringBuilder addParamPrefix(final String baseUrl, final StringBuilder result) {
        // Add '?' if missing and add '&' if params already exist in base url
        final int queryStart = baseUrl.indexOf('?');
        final int lastChar = result.length() - 1;
        if (queryStart == -1) {
            result.append('?');
        } else if (queryStart < lastChar && baseUrl.charAt(lastChar) != '&') {
            result.append('&');
        }
        return result;
    }

    private static StringBuilder addParam(
        final Object key, Object value, final StringBuilder result) {
        if (value != null && value.getClass().isArray()) {
            value = arrayToList(value);
        }

        if (value instanceof Iterable<?>) {
            Iterator<?> iterator = ((Iterable<?>) value).iterator();
            while (iterator.hasNext()) {
                result.append(key);
                result.append("[]=");
                Object element = iterator.next();
                if (element != null) {
                    result.append(element);
                }
                if (iterator.hasNext()) {
                    result.append("&");
                }
            }
        } else {
            result.append(key);
            result.append("=");
            if (value != null) {
                result.append(value);
            }
        }

        return result;
    }

    /**
     * Specify the {@link ConnectionFactory} used to create new requests.
     */
    public static void setConnectionFactory(final ConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            CONNECTION_FACTORY = ConnectionFactory.DEFAULT;
        } else {
            CONNECTION_FACTORY = connectionFactory;
        }
    }

    /**
     * Represents array of any type as list of objects so we can easily iterate over it
     *
     * @param array of elements
     * @return list with the same elements
     */
    private static List<Object> arrayToList(final Object array) {
        if (array instanceof Object[]) {
            return Arrays.asList((Object[]) array);
        }

        List<Object> result = new ArrayList<Object>();
        // Arrays of the primitive types can't be cast to array of Object, so this:
        if (array instanceof int[]) {
            for (int value : (int[]) array) {
                result.add(value);
            }
        } else if (array instanceof boolean[]) {
            for (boolean value : (boolean[]) array) {
                result.add(value);
            }
        } else if (array instanceof long[]) {
            for (long value : (long[]) array) {
                result.add(value);
            }
        } else if (array instanceof float[]) {
            for (float value : (float[]) array) {
                result.add(value);
            }
        } else if (array instanceof double[]) {
            for (double value : (double[]) array) {
                result.add(value);
            }
        } else if (array instanceof short[]) {
            for (short value : (short[]) array) {
                result.add(value);
            }
        } else if (array instanceof byte[]) {
            for (byte value : (byte[]) array) {
                result.add(value);
            }
        } else if (array instanceof char[]) {
            for (char value : (char[]) array) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * Encode the given URL as an ASCII {@link String}
     *
     * <p>This method ensures the path and query segments of the URL are properly encoded such as ' '
     * characters being encoded to '%20' or any UTF-8 characters that are non-ASCII. No encoding of
     * URLs is done by default by the {@link SimpleHttpRequest} constructors and so if URL encoding is
     * needed this method should be called before calling the {@link SimpleHttpRequest} constructor.
     *
     * @param url
     * @return encoded URL
     * @throws HttpRequestException
     */
    public static String encode(final CharSequence url) throws HttpRequestException {
        URL parsed;
        try {
            parsed = new URL(url.toString());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }

        String host = parsed.getHost();
        int port = parsed.getPort();
        if (port != -1) {
            host = host + ':' + Integer.toString(port);
        }

        try {
            String encoded =
                new URI(parsed.getProtocol(), host, parsed.getPath(), parsed.getQuery(), null)
                    .toASCIIString();
            int paramsStart = encoded.indexOf('?');
            if (paramsStart > 0 && paramsStart + 1 < encoded.length()) {
                encoded =
                    encoded.substring(0, paramsStart + 1)
                        + encoded.substring(paramsStart + 1).replace("+", "%2B");
            }
            return encoded;
        } catch (URISyntaxException e) {
            IOException io = new IOException("Parsing URI failed");
            io.initCause(e);
            throw new HttpRequestException(io);
        }
    }

    /**
     * Append given map as query parameters to the base URL
     *
     * <p>Each map entry's key will be a parameter name and the value's {@link Object#toString()} will
     * be the parameter value.
     *
     * @param url
     * @param params
     * @return URL with appended query params
     */
    public static String append(final CharSequence url, final Map<?, ?> params) {
        final String baseUrl = url.toString();
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        final StringBuilder result = new StringBuilder(baseUrl);

        addPathSeparator(baseUrl, result);
        addParamPrefix(baseUrl, result);

        Entry<?, ?> entry;
        Iterator<?> iterator = params.entrySet().iterator();
        entry = (Entry<?, ?>) iterator.next();
        addParam(entry.getKey().toString(), entry.getValue(), result);

        while (iterator.hasNext()) {
            result.append('&');
            entry = (Entry<?, ?>) iterator.next();
            addParam(entry.getKey().toString(), entry.getValue(), result);
        }

        return result.toString();
    }

    /**
     * Append given name/value pairs as query parameters to the base URL
     *
     * <p>The params argument is interpreted as a sequence of name/value pairs so the given number of
     * params must be divisible by 2.
     *
     * @param url
     * @param params name/value pairs
     * @return URL with appended query params
     */
    public static String append(final CharSequence url, final Object... params) {
        final String baseUrl = url.toString();
        if (params == null || params.length == 0) {
            return baseUrl;
        }

        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("Must specify an even number of parameter names/values");
        }

        final StringBuilder result = new StringBuilder(baseUrl);

        addPathSeparator(baseUrl, result);
        addParamPrefix(baseUrl, result);

        addParam(params[0], params[1], result);

        for (int i = 2; i < params.length; i += 2) {
            result.append('&');
            addParam(params[i], params[i + 1], result);
        }

        return result.toString();
    }

    /**
     * Start a 'GET' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest get(final CharSequence url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_GET);
    }

    /**
     * Start a 'GET' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest get(final URL url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_GET);
    }

    /**
     * Start a 'GET' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  The query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest get(
        final CharSequence baseUrl, final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return get(encode ? encode(url) : url);
    }

    /**
     * Start a 'GET' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest get(
        final CharSequence baseUrl, final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return get(encode ? encode(url) : url);
    }

    /**
     * Start a 'POST' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest post(final CharSequence url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_POST);
    }

    /**
     * Start a 'POST' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest post(final URL url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_POST);
    }

    /**
     * Start a 'POST' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  the query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest post(
        final CharSequence baseUrl, final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return post(encode ? encode(url) : url);
    }

    /**
     * Start a 'POST' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest post(
        final CharSequence baseUrl, final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return post(encode ? encode(url) : url);
    }

    /**
     * Start a 'PUT' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest put(final CharSequence url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_PUT);
    }

    /**
     * Start a 'PUT' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest put(final URL url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_PUT);
    }

    /**
     * Start a 'PUT' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  the query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest put(
        final CharSequence baseUrl, final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return put(encode ? encode(url) : url);
    }

    /**
     * Start a 'PUT' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest put(
        final CharSequence baseUrl, final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return put(encode ? encode(url) : url);
    }

    /**
     * Start a 'DELETE' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest delete(final CharSequence url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_DELETE);
    }

    /**
     * Start a 'DELETE' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest delete(final URL url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_DELETE);
    }

    /**
     * Start a 'DELETE' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  The query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest delete(
        final CharSequence baseUrl, final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return delete(encode ? encode(url) : url);
    }

    /**
     * Start a 'DELETE' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest delete(
        final CharSequence baseUrl, final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return delete(encode ? encode(url) : url);
    }

    /**
     * Start a 'HEAD' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest head(final CharSequence url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_HEAD);
    }

    /**
     * Start a 'HEAD' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest head(final URL url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_HEAD);
    }

    /**
     * Start a 'HEAD' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param params  The query parameters to include as part of the baseUrl
     * @param encode  true to encode the full URL
     * @return request
     * @see #append(CharSequence, Map)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest head(
        final CharSequence baseUrl, final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return head(encode ? encode(url) : url);
    }

    /**
     * Start a 'GET' request to the given URL along with the query params
     *
     * @param baseUrl
     * @param encode  true to encode the full URL
     * @param params  the name/value query parameter pairs to include as part of the baseUrl
     * @return request
     * @see #append(CharSequence, Object...)
     * @see #encode(CharSequence)
     */
    public static SimpleHttpRequest head(
        final CharSequence baseUrl, final boolean encode, final Object... params) {
        String url = append(baseUrl, params);
        return head(encode ? encode(url) : url);
    }

    /**
     * Start an 'OPTIONS' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest options(final CharSequence url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_OPTIONS);
    }

    /**
     * Start an 'OPTIONS' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest options(final URL url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_OPTIONS);
    }

    /**
     * Start a 'TRACE' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest trace(final CharSequence url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_TRACE);
    }

    /**
     * Start a 'TRACE' request to the given URL
     *
     * @param url
     * @return request
     * @throws HttpRequestException
     */
    public static SimpleHttpRequest trace(final URL url) throws HttpRequestException {
        return new SimpleHttpRequest(url, METHOD_TRACE);
    }

    /**
     * Set the 'http.keepAlive' property to the given value.
     *
     * <p>This setting will apply to all requests.
     *
     * @param keepAlive
     */
    public static void keepAlive(final boolean keepAlive) {
        setProperty("http.keepAlive", Boolean.toString(keepAlive));
    }

    /**
     * Set the 'http.maxConnections' property to the given value.
     *
     * <p>This setting will apply to all requests.
     *
     * @param maxConnections
     */
    public static void maxConnections(final int maxConnections) {
        setProperty("http.maxConnections", Integer.toString(maxConnections));
    }

    /**
     * Set the 'http.proxyHost' and 'https.proxyHost' properties to the given host value.
     *
     * <p>This setting will apply to all requests.
     *
     * @param host
     */
    public static void proxyHost(final String host) {
        setProperty("http.proxyHost", host);
        setProperty("https.proxyHost", host);
    }

    /**
     * Set the 'http.proxyPort' and 'https.proxyPort' properties to the given port number.
     *
     * <p>This setting will apply to all requests.
     *
     * @param port
     */
    public static void proxyPort(final int port) {
        final String portValue = Integer.toString(port);
        setProperty("http.proxyPort", portValue);
        setProperty("https.proxyPort", portValue);
    }

    /**
     * Set the 'http.nonProxyHosts' property to the given host values.
     *
     * <p>Hosts will be separated by a '|' character.
     *
     * <p>This setting will apply to all requests.
     *
     * @param hosts
     */
    public static void nonProxyHosts(final String... hosts) {
        if (hosts != null && hosts.length > 0) {
            StringBuilder separated = new StringBuilder();
            int last = hosts.length - 1;
            for (int i = 0; i < last; i++) {
                separated.append(hosts[i]).append('|');
            }
            separated.append(hosts[last]);
            setProperty("http.nonProxyHosts", separated.toString());
        } else {
            setProperty("http.nonProxyHosts", null);
        }
    }

    /**
     * Set property to given value.
     *
     * <p>Specifying a null value will cause the property to be cleared
     *
     * @param name
     * @param value
     * @return previous value
     */
    private static String setProperty(final String name, final String value) {
        final PrivilegedAction<String> action;
        if (value != null) {
            action =
                new PrivilegedAction<String>() {

                    public String run() {
                        return System.setProperty(name, value);
                    }
                };
        } else {
            action =
                new PrivilegedAction<String>() {

                    public String run() {
                        return System.clearProperty(name);
                    }
                };
        }
        return AccessController.doPrivileged(action);
    }

    private Proxy createProxy() {
        return new Proxy(HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
    }

    private HttpURLConnection createConnection() {
        try {
            final HttpURLConnection connection;
            if (httpProxyHost != null) {
                connection = CONNECTION_FACTORY.create(url, createProxy());
            } else {
                connection = CONNECTION_FACTORY.create(url);
            }
            connection.setRequestMethod(requestMethod);
            return connection;
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    @Override
    public String toString() {
        return method() + ' ' + url();
    }

    /**
     * Get underlying connection
     *
     * @return connection
     */
    public HttpURLConnection getConnection() {
        if (connection == null) {
            connection = createConnection();
        }
        return connection;
    }

    /**
     * Set whether or not to ignore exceptions that occur from calling {@link Closeable#close()}
     *
     * <p>The default value of this setting is <code>true</code>
     *
     * @param ignore
     * @return this request
     */
    public SimpleHttpRequest ignoreCloseExceptions(final boolean ignore) {
        ignoreCloseExceptions = ignore;
        return this;
    }

    /**
     * Get whether or not exceptions thrown by {@link Closeable#close()} are ignored
     *
     * @return true if ignoring, false if throwing
     */
    public boolean ignoreCloseExceptions() {
        return ignoreCloseExceptions;
    }

    /**
     * Get the status code of the response
     *
     * @return the response code
     * @throws HttpRequestException
     */
    public int code() throws HttpRequestException {
        try {
            closeOutput();
            return getConnection().getResponseCode();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Set the value of the given {@link AtomicInteger} to the status code of the response
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public SimpleHttpRequest code(final AtomicInteger output) throws HttpRequestException {
        output.set(code());
        return this;
    }

    /**
     * Is the response code a 200 OK?
     *
     * @return true if 200, false otherwise
     * @throws HttpRequestException
     */
    public boolean ok() throws HttpRequestException {
        return HTTP_OK == code();
    }

    /**
     * Is the response code a 201 Created?
     *
     * @return true if 201, false otherwise
     * @throws HttpRequestException
     */
    public boolean created() throws HttpRequestException {
        return HTTP_CREATED == code();
    }

    /**
     * Is the response code a 204 No Content?
     *
     * @return true if 204, false otherwise
     * @throws HttpRequestException
     */
    public boolean noContent() throws HttpRequestException {
        return HTTP_NO_CONTENT == code();
    }

    /**
     * Is the response code a 500 Internal Server Error?
     *
     * @return true if 500, false otherwise
     * @throws HttpRequestException
     */
    public boolean serverError() throws HttpRequestException {
        return HTTP_INTERNAL_ERROR == code();
    }

    /**
     * Is the response code a 400 Bad Request?
     *
     * @return true if 400, false otherwise
     * @throws HttpRequestException
     */
    public boolean badRequest() throws HttpRequestException {
        return HTTP_BAD_REQUEST == code();
    }

    /**
     * Is the response code a 404 Not Found?
     *
     * @return true if 404, false otherwise
     * @throws HttpRequestException
     */
    public boolean notFound() throws HttpRequestException {
        return HTTP_NOT_FOUND == code();
    }

    /**
     * Is the response code a 304 Not Modified?
     *
     * @return true if 304, false otherwise
     * @throws HttpRequestException
     */
    public boolean notModified() throws HttpRequestException {
        return HTTP_NOT_MODIFIED == code();
    }

    /**
     * Get status message of the response
     *
     * @return message
     * @throws HttpRequestException
     */
    public String message() throws HttpRequestException {
        try {
            closeOutput();
            return getConnection().getResponseMessage();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Disconnect the connection
     *
     * @return this request
     */
    public SimpleHttpRequest disconnect() {
        getConnection().disconnect();
        return this;
    }

    /**
     * Set chunked streaming mode to the given size
     *
     * @param size
     * @return this request
     */
    public SimpleHttpRequest chunk(final int size) {
        getConnection().setChunkedStreamingMode(size);
        return this;
    }

    /**
     * Set the size used when buffering and copying between streams
     *
     * <p>This size is also used for send and receive buffers created for both char and byte arrays
     *
     * <p>The default buffer size is 8,192 bytes
     *
     * @param size
     * @return this request
     */
    public SimpleHttpRequest bufferSize(final int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        bufferSize = size;
        return this;
    }

    /**
     * Get the configured buffer size
     *
     * <p>The default buffer size is 8,192 bytes
     *
     * @return buffer size
     */
    public int bufferSize() {
        return bufferSize;
    }

    /**
     * Set whether or not the response body should be automatically uncompressed when read from.
     *
     * <p>This will only affect requests that have the 'Content-Encoding' response header set to
     * 'gzip'.
     *
     * <p>This causes all receive methods to use a {@link GZIPInputStream} when applicable so that
     * higher level streams and readers can read the data uncompressed.
     *
     * <p>Setting this option does not cause any request headers to be set automatically so {@link
     * #acceptGzipEncoding()} should be used in conjunction with this setting to tell the server to
     * gzip the response.
     *
     * @param uncompress
     * @return this request
     */
    public SimpleHttpRequest uncompress(final boolean uncompress) {
        this.uncompress = uncompress;
        return this;
    }

    /**
     * Create byte array output stream
     *
     * @return stream
     */
    protected ByteArrayOutputStream byteStream() {
        final int size = contentLength();
        if (size > 0) {
            return new ByteArrayOutputStream(size);
        } else {
            return new ByteArrayOutputStream();
        }
    }

    /**
     * Get response as {@link String} in given character set
     *
     * <p>This will fall back to using the UTF-8 character set if the given charset is null
     *
     * @param charset
     * @return string
     * @throws HttpRequestException
     */
    public String body(final String charset) throws HttpRequestException {
        final ByteArrayOutputStream output = byteStream();
        try {
            copy(buffer(), output);
            return output.toString(getValidCharset(charset));
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Get response as {@link String} using character set returned from {@link #charset()}
     *
     * @return string
     * @throws HttpRequestException
     */
    public String body() throws HttpRequestException {
        return body(charset());
    }

    /**
     * Get the response body as a {@link String} and set it as the value of the given reference.
     *
     * @param output
     * @return this request
     * @throws HttpRequestException
     */
    public SimpleHttpRequest body(final AtomicReference<String> output) throws HttpRequestException {
        output.set(body());
        return this;
    }

    /**
     * Get the response body as a {@link String} and set it as the value of the given reference.
     *
     * @param output
     * @param charset
     * @return this request
     * @throws HttpRequestException
     */
    public SimpleHttpRequest body(final AtomicReference<String> output, final String charset)
        throws HttpRequestException {
        output.set(body(charset));
        return this;
    }

    /**
     * Is the response body empty?
     *
     * @return true if the Content-Length response header is 0, false otherwise
     * @throws HttpRequestException
     */
    public boolean isBodyEmpty() throws HttpRequestException {
        return contentLength() == 0;
    }

    /**
     * Get response as byte array
     *
     * @return byte array
     * @throws HttpRequestException
     */
    public byte[] bytes() throws HttpRequestException {
        final ByteArrayOutputStream output = byteStream();
        try {
            copy(buffer(), output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return output.toByteArray();
    }

    /**
     * Get response in a buffered stream
     *
     * @return stream
     * @throws HttpRequestException
     * @see #bufferSize(int)
     */
    public BufferedInputStream buffer() throws HttpRequestException {
        return new BufferedInputStream(stream(), bufferSize);
    }

    /**
     * Get stream to response body
     *
     * @return stream
     * @throws HttpRequestException
     */
    public InputStream stream() throws HttpRequestException {
        InputStream stream;
        if (code() < HTTP_BAD_REQUEST) {
            try {
                stream = getConnection().getInputStream();
            } catch (IOException e) {
                throw new HttpRequestException(e);
            }
        } else {
            stream = getConnection().getErrorStream();
            if (stream == null) {
                try {
                    stream = getConnection().getInputStream();
                } catch (IOException e) {
                    if (contentLength() > 0) {
                        throw new HttpRequestException(e);
                    } else {
                        stream = new ByteArrayInputStream(new byte[0]);
                    }
                }
            }
        }

        if (!uncompress || !ENCODING_GZIP.equals(contentEncoding())) {
            return stream;
        } else {
            try {
                return new GZIPInputStream(stream);
            } catch (IOException e) {
                throw new HttpRequestException(e);
            }
        }
    }

    /**
     * Get reader to response body using given character set.
     *
     * <p>This will fall back to using the UTF-8 character set if the given charset is null
     */
    public InputStreamReader reader(final String charset) throws HttpRequestException {
        try {
            return new InputStreamReader(stream(), getValidCharset(charset));
        } catch (UnsupportedEncodingException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Get reader to response body using the character set returned from {@link #charset()}
     */
    public InputStreamReader reader() throws HttpRequestException {
        return reader(charset());
    }

    /**
     * Get buffered reader to response body using the given character set r and the configured buffer
     * size
     * @see #bufferSize(int)
     */
    public BufferedReader bufferedReader(final String charset) throws HttpRequestException {
        return new BufferedReader(reader(charset), bufferSize);
    }

    /**
     * Get buffered reader to response body using the character set returned from {@link #charset()}
     * and the configured buffer size
     * @see #bufferSize(int)
     */
    public BufferedReader bufferedReader() throws HttpRequestException {
        return bufferedReader(charset());
    }

    /**
     * Stream response body to file
     */
    public SimpleHttpRequest receive(final File file) throws HttpRequestException {
        final OutputStream output;
        try {
            output = new BufferedOutputStream(new FileOutputStream(file), bufferSize);
        } catch (FileNotFoundException e) {
            throw new HttpRequestException(e);
        }
        return new AbstractCloseOperation<SimpleHttpRequest>(output, ignoreCloseExceptions) {

            @Override
            protected SimpleHttpRequest run() throws HttpRequestException, IOException {
                return receive(output);
            }
        }.call();
    }

    /**
     * Stream response to given output stream
     */
    public SimpleHttpRequest receive(final OutputStream output) throws HttpRequestException {
        try {
            return copy(buffer(), output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Stream response to given print stream
     */
    public SimpleHttpRequest receive(final PrintStream output) throws HttpRequestException {
        return receive((OutputStream) output);
    }

    /**
     * Receive response into the given appendable
     */
    public SimpleHttpRequest receive(final Appendable appendable) throws HttpRequestException {
        final BufferedReader reader = bufferedReader();
        return new AbstractCloseOperation<SimpleHttpRequest>(reader, ignoreCloseExceptions) {

            @Override
            public SimpleHttpRequest run() throws IOException {
                final CharBuffer buffer = CharBuffer.allocate(bufferSize);
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    buffer.rewind();
                    appendable.append(buffer, 0, read);
                    buffer.rewind();
                }
                return SimpleHttpRequest.this;
            }
        }.call();
    }

    /**
     * Receive response into the given writer
     */
    public SimpleHttpRequest receive(final Writer writer) throws HttpRequestException {
        final BufferedReader reader = bufferedReader();
        return new AbstractCloseOperation<SimpleHttpRequest>(reader, ignoreCloseExceptions) {

            @Override
            public SimpleHttpRequest run() throws IOException {
                return copy(reader, writer);
            }
        }.call();
    }

    /**
     * Set read timeout on connection to given value
     *
     * @param timeout
     * @return this request
     */
    public SimpleHttpRequest readTimeout(final int timeout) {
        getConnection().setReadTimeout(timeout);
        return this;
    }

    /**
     * Set connect timeout on connection to given value
     *
     * @param timeout
     * @return this request
     */
    public SimpleHttpRequest connectTimeout(final int timeout) {
        getConnection().setConnectTimeout(timeout);
        return this;
    }

    /**
     * Set header name to given value
     *
     * @param name
     * @param value
     * @return this request
     */
    public SimpleHttpRequest header(final String name, final String value) {
        getConnection().setRequestProperty(name, value);
        return this;
    }

    /**
     * Set header name to given value
     *
     * @param name
     * @param value
     * @return this request
     */
    public SimpleHttpRequest header(final String name, final Number value) {
        return header(name, value != null ? value.toString() : null);
    }

    /**
     * Set all headers found in given map where the keys are the header names and the values are the
     * header values
     *
     * @param headers
     * @return this request
     */
    public SimpleHttpRequest headers(final Map<String, String> headers) {
        if (!headers.isEmpty()) {
            for (Entry<String, String> header : headers.entrySet()) {
                header(header);
            }
        }
        return this;
    }

    /**
     * Set header to have given entry's key as the name and value as the value
     *
     * @param header
     * @return this request
     */
    public SimpleHttpRequest header(final Entry<String, String> header) {
        return header(header.getKey(), header.getValue());
    }

    /**
     * Get a response header
     *
     * @param name
     * @return response header
     * @throws HttpRequestException
     */
    public String header(final String name) throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderField(name);
    }

    /**
     * Get all the response headers
     *
     * @return map of response header names to their value(s)
     * @throws HttpRequestException
     */
    public Map<String, List<String>> headers() throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFields();
    }

    /**
     * Get a date header from the response falling back to returning -1 if the header is missing or
     * parsing fails
     *
     * @param name
     * @return date, -1 on failures
     * @throws HttpRequestException
     */
    public long dateHeader(final String name) throws HttpRequestException {
        return dateHeader(name, -1L);
    }

    /**
     * Get a date header from the response falling back to returning the given default value if the
     * header is missing or parsing fails
     */
    public long dateHeader(final String name, final long defaultValue) throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFieldDate(name, defaultValue);
    }

    /**
     * Get an integer header from the response falling back to returning -1 if the header is missing
     * or parsing fails
     *
     * @param name
     * @return header value as an integer, -1 when missing or parsing fails
     * @throws HttpRequestException
     */
    public int intHeader(final String name) throws HttpRequestException {
        return intHeader(name, -1);
    }

    /**
     * Get an integer header value from the response falling back to the given default value if the
     * header is missing or if parsing fails
     */
    public int intHeader(final String name, final int defaultValue) throws HttpRequestException {
        closeOutputQuietly();
        return getConnection().getHeaderFieldInt(name, defaultValue);
    }

    /**
     * Get all values of the given header from the response
     *
     * @param name
     * @return non-null but possibly empty array of {@link String} header values
     */
    public String[] headers(final String name) {
        final Map<String, List<String>> headers = headers();
        if (headers == null || headers.isEmpty()) {
            return EMPTY_STRINGS;
        }

        final List<String> values = headers.get(name);
        if (values != null && !values.isEmpty()) {
            return values.toArray(new String[values.size()]);
        } else {
            return EMPTY_STRINGS;
        }
    }

    /**
     * Get parameter with given name from header value in response
     *
     * @param headerName
     * @param paramName
     * @return parameter value or null if missing
     */
    public String parameter(final String headerName, final String paramName) {
        return getParam(header(headerName), paramName);
    }

    /**
     * Get all parameters from header value in response
     *
     * <p>This will be all key=value pairs after the first ';' that are separated by a ';'
     *
     * @param headerName
     * @return non-null but possibly empty map of parameter headers
     */
    public Map<String, String> parameters(final String headerName) {
        return getParams(header(headerName));
    }

    /**
     * Get parameter values from header value
     *
     * @param header
     * @return parameter value or null if none
     */
    protected Map<String, String> getParams(final String header) {
        if (header == null || header.length() == 0) {
            return Collections.emptyMap();
        }

        final int headerLength = header.length();
        int start = header.indexOf(';') + 1;
        if (start == 0 || start == headerLength) {
            return Collections.emptyMap();
        }

        int end = header.indexOf(';', start);
        if (end == -1) {
            end = headerLength;
        }

        Map<String, String> params = new LinkedHashMap<String, String>();
        while (start < end) {
            int nameEnd = header.indexOf('=', start);
            if (nameEnd != -1 && nameEnd < end) {
                String name = header.substring(start, nameEnd).trim();
                if (name.length() > 0) {
                    String value = header.substring(nameEnd + 1, end).trim();
                    int length = value.length();
                    if (length != 0) {
                        if (length > 2 && '"' == value.charAt(0) && '"' == value.charAt(length - 1)) {
                            params.put(name, value.substring(1, length - 1));
                        } else {
                            params.put(name, value);
                        }
                    }
                }
            }

            start = end + 1;
            end = header.indexOf(';', start);
            if (end == -1) {
                end = headerLength;
            }
        }

        return params;
    }

    /**
     * Get parameter value from header value
     *
     * @param value
     * @param paramName
     * @return parameter value or null if none
     */
    protected String getParam(final String value, final String paramName) {
        if (value == null || value.length() == 0) {
            return null;
        }

        final int length = value.length();
        int start = value.indexOf(';') + 1;
        if (start == 0 || start == length) {
            return null;
        }

        int end = value.indexOf(';', start);
        if (end == -1) {
            end = length;
        }

        while (start < end) {
            int nameEnd = value.indexOf('=', start);
            if (nameEnd != -1
                && nameEnd < end
                && paramName.equals(value.substring(start, nameEnd).trim())) {
                String paramValue = value.substring(nameEnd + 1, end).trim();
                int valueLength = paramValue.length();
                if (valueLength != 0) {
                    if (valueLength > 2
                        && '"' == paramValue.charAt(0)
                        && '"' == paramValue.charAt(valueLength - 1)) {
                        return paramValue.substring(1, valueLength - 1);
                    } else {
                        return paramValue;
                    }
                }
            }

            start = end + 1;
            end = value.indexOf(';', start);
            if (end == -1) {
                end = length;
            }
        }

        return null;
    }

    /**
     * Get 'charset' parameter from 'Content-Type' response header
     *
     * @return charset or null if none
     */
    public String charset() {
        return parameter(HEADER_CONTENT_TYPE, PARAM_CHARSET);
    }

    /**
     * Set the 'User-Agent' header to given value
     *
     * @param userAgent
     * @return this request
     */
    public SimpleHttpRequest userAgent(final String userAgent) {
        return header(HEADER_USER_AGENT, userAgent);
    }

    /**
     * Set the 'Referer' header to given value
     *
     * @param referer
     * @return this request
     */
    public SimpleHttpRequest referer(final String referer) {
        return header(HEADER_REFERER, referer);
    }

    /**
     * Set value of {@link HttpURLConnection#setUseCaches(boolean)}
     *
     * @param useCaches
     * @return this request
     */
    public SimpleHttpRequest useCaches(final boolean useCaches) {
        getConnection().setUseCaches(useCaches);
        return this;
    }

    /**
     * Set the 'Accept-Encoding' header to given value
     *
     * @param acceptEncoding
     * @return this request
     */
    public SimpleHttpRequest acceptEncoding(final String acceptEncoding) {
        return header(HEADER_ACCEPT_ENCODING, acceptEncoding);
    }

    /**
     * Set the 'Accept-Encoding' header to 'gzip'
     *
     * @return this request
     * @see #uncompress(boolean)
     */
    public SimpleHttpRequest acceptGzipEncoding() {
        return acceptEncoding(ENCODING_GZIP);
    }

    /**
     * Set the 'Accept-Charset' header to given value
     *
     * @param acceptCharset
     * @return this request
     */
    public SimpleHttpRequest acceptCharset(final String acceptCharset) {
        return header(HEADER_ACCEPT_CHARSET, acceptCharset);
    }

    /**
     * Get the 'Content-Encoding' header from the response
     *
     * @return this request
     */
    public String contentEncoding() {
        return header(HEADER_CONTENT_ENCODING);
    }

    /**
     * Get the 'Server' header from the response
     *
     * @return server
     */
    public String server() {
        return header(HEADER_SERVER);
    }

    /**
     * Get the 'Date' header from the response
     *
     * @return date value, -1 on failures
     */
    public long date() {
        return dateHeader(HEADER_DATE);
    }

    /**
     * Get the 'Cache-Control' header from the response
     *
     * @return cache control
     */
    public String cacheControl() {
        return header(HEADER_CACHE_CONTROL);
    }

    /**
     * Get the 'ETag' header from the response
     *
     * @return entity tag
     */
    public String eTag() {
        return header(HEADER_ETAG);
    }

    /**
     * Get the 'Expires' header from the response
     *
     * @return expires value, -1 on failures
     */
    public long expires() {
        return dateHeader(HEADER_EXPIRES);
    }

    /**
     * Get the 'Last-Modified' header from the response
     *
     * @return last modified value, -1 on failures
     */
    public long lastModified() {
        return dateHeader(HEADER_LAST_MODIFIED);
    }

    /**
     * Get the 'Location' header from the response
     *
     * @return location
     */
    public String location() {
        return header(HEADER_LOCATION);
    }

    /**
     * Set the 'Authorization' header to given value
     *
     * @param authorization
     * @return this request
     */
    public SimpleHttpRequest authorization(final String authorization) {
        return header(HEADER_AUTHORIZATION, authorization);
    }

    /**
     * Set the 'Proxy-Authorization' header to given value
     *
     * @param proxyAuthorization
     * @return this request
     */
    public SimpleHttpRequest proxyAuthorization(final String proxyAuthorization) {
        return header(HEADER_PROXY_AUTHORIZATION, proxyAuthorization);
    }

    /**
     * Set the 'Authorization' header to given values in Basic authentication format
     *
     * @param name
     * @param password
     * @return this request
     */
    public SimpleHttpRequest basic(final String name, final String password) {
        return authorization("Basic " + Base64.encode(name + ':' + password));
    }

    /**
     * Set the 'Proxy-Authorization' header to given values in Basic authentication format
     *
     * @param name
     * @param password
     * @return this request
     */
    public SimpleHttpRequest proxyBasic(final String name, final String password) {
        return proxyAuthorization("Basic " + Base64.encode(name + ':' + password));
    }

    /**
     * Set the 'If-Modified-Since' request header to the given value
     *
     * @param ifModifiedSince
     * @return this request
     */
    public SimpleHttpRequest ifModifiedSince(final long ifModifiedSince) {
        getConnection().setIfModifiedSince(ifModifiedSince);
        return this;
    }

    /**
     * Set the 'If-None-Match' request header to the given value
     *
     * @param ifNoneMatch
     * @return this request
     */
    public SimpleHttpRequest ifNoneMatch(final String ifNoneMatch) {
        return header(HEADER_IF_NONE_MATCH, ifNoneMatch);
    }

    /**
     * Set the 'Content-Type' request header to the given value
     *
     * @param contentType
     * @return this request
     */
    public SimpleHttpRequest contentType(final String contentType) {
        return contentType(contentType, null);
    }

    /**
     * Set the 'Content-Type' request header to the given value and charset
     *
     * @param contentType
     * @param charset
     * @return this request
     */
    public SimpleHttpRequest contentType(final String contentType, final String charset) {
        if (charset != null && charset.length() > 0) {
            final String separator = "; " + PARAM_CHARSET + '=';
            return header(HEADER_CONTENT_TYPE, contentType + separator + charset);
        } else {
            return header(HEADER_CONTENT_TYPE, contentType);
        }
    }

    /**
     * Get the 'Content-Type' header from the response
     *
     * @return response header value
     */
    public String contentType() {
        return header(HEADER_CONTENT_TYPE);
    }

    /**
     * Get the 'Content-Length' header from the response
     *
     * @return response header value
     */
    public int contentLength() {
        return intHeader(HEADER_CONTENT_LENGTH);
    }

    /**
     * Set the 'Content-Length' request header to the given value
     *
     * @param contentLength
     * @return this request
     */
    public SimpleHttpRequest contentLength(final String contentLength) {
        return contentLength(Integer.parseInt(contentLength));
    }

    /**
     * Set the 'Content-Length' request header to the given value
     */
    public SimpleHttpRequest contentLength(final int contentLength) {
        getConnection().setFixedLengthStreamingMode(contentLength);
        return this;
    }

    /**
     * Set the 'Accept' header to given value
     *
     * @param accept
     * @return this request
     */
    public SimpleHttpRequest accept(final String accept) {
        return header(HEADER_ACCEPT, accept);
    }

    /**
     * Set the 'Accept' header to 'application/json'
     *
     * @return this request
     */
    public SimpleHttpRequest acceptJson() {
        return accept(CONTENT_TYPE_JSON);
    }

    /**
     * Copy from input stream to output stream
     *
     * @param input
     * @param output
     * @return this request
     * @throws IOException
     */
    protected SimpleHttpRequest copy(final InputStream input, final OutputStream output)
        throws IOException {
        return new AbstractCloseOperation<SimpleHttpRequest>(input, ignoreCloseExceptions) {

            @Override
            public SimpleHttpRequest run() throws IOException {
                final byte[] buffer = new byte[bufferSize];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    totalWritten += read;
                    progress.onUpload(totalWritten, totalSize);
                }
                return SimpleHttpRequest.this;
            }
        }.call();
    }

    /**
     * Copy from reader to writer
     *
     * @param input
     * @param output
     * @return this request
     * @throws IOException
     */
    protected SimpleHttpRequest copy(final Reader input, final Writer output) throws IOException {
        return new AbstractCloseOperation<SimpleHttpRequest>(input, ignoreCloseExceptions) {

            @Override
            public SimpleHttpRequest run() throws IOException {
                final char[] buffer = new char[bufferSize];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    totalWritten += read;
                    progress.onUpload(totalWritten, -1);
                }
                return SimpleHttpRequest.this;
            }
        }.call();
    }

    /**
     * Set the UploadProgress callback for this request
     *
     * @param callback
     * @return this request
     */
    public SimpleHttpRequest progress(final UploadProgress callback) {
        if (callback == null) {
            progress = UploadProgress.DEFAULT;
        } else {
            progress = callback;
        }
        return this;
    }

    private SimpleHttpRequest incrementTotalSize(final long size) {
        if (totalSize == -1) {
            totalSize = 0;
        }
        totalSize += size;
        return this;
    }

    /**
     * Close output stream
     */
    protected SimpleHttpRequest closeOutput() throws IOException {
        progress(null);
        if (output == null) {
            return this;
        }
        if (multipart) {
            output.write(CRLF + "--" + BOUNDARY + "--" + CRLF);
        }
        if (ignoreCloseExceptions) {
            try {
                output.close();
            } catch (IOException ignored) {
                // Ignored
            }
        } else {
            output.close();
        }
        output = null;
        return this;
    }

    /**
     * Call {@link #closeOutput()} and re-throw a caught {@link IOException}s as an {@link
     * HttpRequestException}
     *
     * @return this request
     * @throws HttpRequestException
     */
    protected SimpleHttpRequest closeOutputQuietly() throws HttpRequestException {
        try {
            return closeOutput();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Open output stream
     */
    protected SimpleHttpRequest openOutput() throws IOException {
        if (output != null) {
            return this;
        }
        getConnection().setDoOutput(true);
        final String charset =
            getParam(getConnection().getRequestProperty(HEADER_CONTENT_TYPE), PARAM_CHARSET);
        output = new RequestOutputStream(getConnection().getOutputStream(), charset, bufferSize);
        return this;
    }

    /**
     * Start part of a multipart
     */
    protected SimpleHttpRequest startPart() throws IOException {
        if (!multipart) {
            multipart = true;
            contentType(CONTENT_TYPE_MULTIPART).openOutput();
            output.write("--" + BOUNDARY + CRLF);
        } else {
            output.write(CRLF + "--" + BOUNDARY + CRLF);
        }
        return this;
    }

    /**
     * Write part header
     */
    protected SimpleHttpRequest writePartHeader(final String name, final String filename)
        throws IOException {
        return writePartHeader(name, filename, null);
    }

    /**
     * Write part header
     */
    protected SimpleHttpRequest writePartHeader(
        final String name, final String filename, final String contentType) throws IOException {
        final StringBuilder partBuffer = new StringBuilder();
        partBuffer.append("form-data; name=\"").append(name);
        if (filename != null) {
            partBuffer.append("\"; filename=\"").append(filename);
        }
        partBuffer.append('"');
        partHeader("Content-Disposition", partBuffer.toString());
        if (contentType != null) {
            partHeader(HEADER_CONTENT_TYPE, contentType);
        }
        return send(CRLF);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(final String name, final String part) {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(final String name, final String filename, final String part)
        throws HttpRequestException {
        return part(name, filename, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(
        final String name, final String filename, final String contentType, final String part)
        throws HttpRequestException {
        try {
            startPart();
            writePartHeader(name, filename, contentType);
            output.write(part);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(final String name, final Number part) throws HttpRequestException {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(final String name, final String filename, final Number part)
        throws HttpRequestException {
        return part(name, filename, part != null ? part.toString() : null);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(final String name, final File part) throws HttpRequestException {
        return part(name, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(final String name, final String filename, final File part)
        throws HttpRequestException {
        return part(name, filename, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(
        final String name, final String filename, final String contentType, final File part)
        throws HttpRequestException {
        final InputStream stream;
        try {
            stream = new BufferedInputStream(new FileInputStream(part));
            incrementTotalSize(part.length());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return part(name, filename, contentType, stream);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(final String name, final InputStream part) throws HttpRequestException {
        return part(name, null, null, part);
    }

    /**
     * Write part of a multipart request to the request body
     */
    public SimpleHttpRequest part(
        final String name, final String filename, final String contentType, final InputStream part)
        throws HttpRequestException {
        try {
            startPart();
            writePartHeader(name, filename, contentType);
            copy(part, output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write a multipart header to the response body
     */
    public SimpleHttpRequest partHeader(final String name, final String value) throws HttpRequestException {
        return send(name).send(": ").send(value).send(CRLF);
    }

    /**
     * Write contents of file to request body
     */
    public SimpleHttpRequest send(final File input) throws HttpRequestException {
        final InputStream stream;
        try {
            stream = new BufferedInputStream(new FileInputStream(input));
            incrementTotalSize(input.length());
        } catch (FileNotFoundException e) {
            throw new HttpRequestException(e);
        }
        return send(stream);
    }

    /**
     * Write byte array to request body
     */
    public SimpleHttpRequest send(final byte[] input) throws HttpRequestException {
        if (input != null) {
            incrementTotalSize(input.length);
        }
        return send(new ByteArrayInputStream(input));
    }

    /**
     * Write stream to request body
     *
     * <p>The given stream will be closed once sending completes
     */
    public SimpleHttpRequest send(final InputStream input) throws HttpRequestException {
        try {
            openOutput();
            copy(input, output);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write reader to request body
     *
     * <p>The given reader will be closed once sending completes
     */
    public SimpleHttpRequest send(final Reader input) throws HttpRequestException {
        try {
            openOutput();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        final Writer writer = new OutputStreamWriter(output, output.encoder.charset());
        return new AbstractFlushOperation<SimpleHttpRequest>(writer) {

            @Override
            protected SimpleHttpRequest run() throws IOException {
                return copy(input, writer);
            }
        }.call();
    }

    /**
     * Write char sequence to request body
     *
     * <p>The charset configured via {@link #contentType(String)} will be used and UTF-8 will be used
     * if it is unset.
     */
    public SimpleHttpRequest send(final CharSequence value) throws HttpRequestException {
        try {
            openOutput();
            output.write(value.toString());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Create writer to request output stream
     */
    public OutputStreamWriter writer() throws HttpRequestException {
        try {
            openOutput();
            return new OutputStreamWriter(output, output.encoder.charset());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    /**
     * Write the values in the map as form data to the request body
     *
     * <p>The pairs specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public SimpleHttpRequest form(final Map<?, ?> values) throws HttpRequestException {
        return form(values, CHARSET_UTF8);
    }

    /**
     * Write the key and value in the entry as form data to the request body
     *
     * <p>The pair specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public SimpleHttpRequest form(final Entry<?, ?> entry) throws HttpRequestException {
        return form(entry, CHARSET_UTF8);
    }

    /**
     * Write the key and value in the entry as form data to the request body
     *
     * <p>The pair specified will be URL-encoded and sent with the 'application/x-www-form-urlencoded'
     * content-type
     */
    public SimpleHttpRequest form(final Entry<?, ?> entry, final String charset)
        throws HttpRequestException {
        return form(entry.getKey(), entry.getValue(), charset);
    }

    /**
     * Write the name/value pair as form data to the request body
     *
     * <p>The pair specified will be URL-encoded in UTF-8 and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public SimpleHttpRequest form(final Object name, final Object value) throws HttpRequestException {
        return form(name, value, CHARSET_UTF8);
    }

    /**
     * Write the name/value pair as form data to the request body
     *
     * <p>The values specified will be URL-encoded and sent with the
     * 'application/x-www-form-urlencoded' content-type
     */
    public SimpleHttpRequest form(final Object name, final Object value, String charset)
        throws HttpRequestException {
        final boolean first = !form;
        if (first) {
            contentType(CONTENT_TYPE_FORM, charset);
            form = true;
        }
        charset = getValidCharset(charset);
        try {
            openOutput();
            if (!first) {
                output.write('&');
            }
            output.write(URLEncoder.encode(name.toString(), charset));
            output.write('=');
            if (value != null) {
                output.write(URLEncoder.encode(value.toString(), charset));
            }
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    /**
     * Write the values in the map as encoded form data to the request body
     */
    public SimpleHttpRequest form(final Map<?, ?> values, final String charset)
        throws HttpRequestException {
        if (!values.isEmpty()) {
            for (Entry<?, ?> entry : values.entrySet()) {
                form(entry, charset);
            }
        }
        return this;
    }

    /**
     * Get the {@link URL} of this request's connection
     *
     * @return request URL
     */
    public URL url() {
        return getConnection().getURL();
    }

    /**
     * Get the HTTP method of this request
     *
     * @return method
     */
    public String method() {
        return getConnection().getRequestMethod();
    }

    /**
     * Configure an HTTP proxy on this connection. Use {{@link #proxyBasic(String, String)} if this
     * proxy requires basic authentication.
     */
    public SimpleHttpRequest useProxy(final String proxyHost, final int proxyPort) {
        if (connection != null) {
            throw new IllegalStateException(
                "The connection has already been created. This method must be called before reading or writing to the request.");
        }

        this.httpProxyHost = proxyHost;
        this.httpProxyPort = proxyPort;
        return this;
    }

    /**
     * Set whether or not the underlying connection should follow redirects in the response.
     *
     * @param followRedirects - true fo follow redirects, false to not.
     * @return this request
     */
    public SimpleHttpRequest followRedirects(final boolean followRedirects) {
        getConnection().setInstanceFollowRedirects(followRedirects);
        return this;
    }

    /**
     * Creates {@link HttpURLConnection HTTP connections} for {@link URL urls}.
     */
    public interface ConnectionFactory {
        /**
         * A {@link ConnectionFactory} which uses the built-in {@link URL#openConnection()}
         */
        ConnectionFactory DEFAULT =
            new ConnectionFactory() {
                public HttpURLConnection create(URL url) throws IOException {
                    return (HttpURLConnection) url.openConnection();
                }

                public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
                    return (HttpURLConnection) url.openConnection(proxy);
                }
            };

        /**
         * Open an {@link HttpURLConnection} for the specified {@link URL}.
         *
         * @throws IOException
         */
        HttpURLConnection create(URL url) throws IOException;

        /**
         * Open an {@link HttpURLConnection} for the specified {@link URL} and {@link Proxy}.
         *
         * @throws IOException
         */
        HttpURLConnection create(URL url, Proxy proxy) throws IOException;
    }

    /**
     * Callback interface for reporting upload progress for a request.
     */
    public interface UploadProgress {
        UploadProgress DEFAULT = new UploadProgress() {
            @Override public void onUpload(long uploaded, long total) {
            }
        };

        /**
         * Callback invoked as data is uploaded by the request.
         *
         * @param uploaded The number of bytes already uploaded
         * @param total    The total number of bytes that will be uploaded or -1 if the length is unknown.
         */
        void onUpload(long uploaded, long total);
    }

    /**
     * Encodes and decodes to and from Base64 notation.
     *
     * <p>I am placing this code in the Public Domain. Do with it as you will. This software comes
     * with no guarantees or warranties but with plenty of well-wishing instead! Please visit <a
     * href="http://iharder.net/base64">http://iharder.net/base64</a> periodically to check for
     * updates or to contribute improvements.
     *
     * @author Robert Harder
     * @author rob@iharder.net
     * @version 2.3.7
     */
    public static class Base64 {

        /**
         * The equals sign (=) as a byte.
         */
        private static final byte EQUALS_SIGN = (byte) '=';

        /**
         * Preferred encoding.
         */
        private static final String PREFERRED_ENCODING = "US-ASCII";

        /**
         * The 64 valid Base64 values.
         */
        private static final byte[] STANDARD_ALPHABET = {
            (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F', (byte) 'G', (byte) 'H',
            (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P',
            (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X',
            (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f',
            (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',
            (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u', (byte) 'v',
            (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3',
            (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) '+', (byte) '/'
        };

        /**
         * Defeats instantiation.
         */
        private Base64() {
        }

        /**
         * Encodes up to three bytes of the array <var>source</var> and writes the resulting four Base64
         * bytes to <var>destination</var>. The source and destination arrays can be manipulated
         * anywhere along their length by specifying <var>srcOffset</var> and <var>destOffset</var>.
         * This method does not check to make sure your arrays are large enough to accomodate
         * <var>srcOffset</var> + 3 for the <var>source</var> array or <var>destOffset</var> + 4 for the
         * <var>destination</var> array. The actual number of significant bytes in your array is given
         * by <var>numSigBytes</var>.
         *
         * <p>This is the lowest level of the encoding methods with all possible parameters.
         *
         * @param source      the array to convert
         * @param srcOffset   the index where conversion begins
         * @param numSigBytes the number of significant bytes in your array
         * @param destination the array to hold the conversion
         * @param destOffset  the index where output will be put
         * @return the <var>destination</var> array
         * @since 1.3
         */
        private static byte[] encode3to4(
            byte[] source, int srcOffset, int numSigBytes, byte[] destination, int destOffset) {

            byte[] alphabet = STANDARD_ALPHABET;

            int inBuff =
                (numSigBytes > 0 ? ((source[srcOffset] << 24) >>> 8) : 0)
                    | (numSigBytes > 1 ? ((source[srcOffset + 1] << 24) >>> 16) : 0)
                    | (numSigBytes > 2 ? ((source[srcOffset + 2] << 24) >>> 24) : 0);

            switch (numSigBytes) {
                case 3:
                    destination[destOffset] = alphabet[inBuff >>> 18];
                    destination[destOffset + 1] = alphabet[(inBuff >>> 12) & 0x3f];
                    destination[destOffset + 2] = alphabet[(inBuff >>> 6) & 0x3f];
                    destination[destOffset + 3] = alphabet[inBuff & 0x3f];
                    return destination;

                case 2:
                    destination[destOffset] = alphabet[inBuff >>> 18];
                    destination[destOffset + 1] = alphabet[(inBuff >>> 12) & 0x3f];
                    destination[destOffset + 2] = alphabet[(inBuff >>> 6) & 0x3f];
                    destination[destOffset + 3] = EQUALS_SIGN;
                    return destination;

                case 1:
                    destination[destOffset] = alphabet[inBuff >>> 18];
                    destination[destOffset + 1] = alphabet[(inBuff >>> 12) & 0x3f];
                    destination[destOffset + 2] = EQUALS_SIGN;
                    destination[destOffset + 3] = EQUALS_SIGN;
                    return destination;

                default:
                    return destination;
            }
        }

        /**
         * Encode string as a byte array in Base64 annotation.
         *
         * @param string
         * @return The Base64-encoded data as a string
         */
        public static String encode(String string) {
            byte[] bytes;
            try {
                bytes = string.getBytes(PREFERRED_ENCODING);
            } catch (UnsupportedEncodingException e) {
                bytes = string.getBytes();
            }
            return encodeBytes(bytes);
        }

        /**
         * Encodes a byte array into Base64 notation.
         *
         * @param source The data to convert
         * @return The Base64-encoded data as a String
         * @throws NullPointerException     if source array is null
         * @throws IllegalArgumentException if source array, offset, or length are invalid
         * @since 2.0
         */
        public static String encodeBytes(byte[] source) {
            return encodeBytes(source, 0, source.length);
        }

        /**
         * Encodes a byte array into Base64 notation.
         *
         * @param source The data to convert
         * @param off    Offset in array where conversion should begin
         * @param len    Length of data to convert
         * @return The Base64-encoded data as a String
         * @throws NullPointerException     if source array is null
         * @throws IllegalArgumentException if source array, offset, or length are invalid
         * @since 2.0
         */
        public static String encodeBytes(byte[] source, int off, int len) {
            byte[] encoded = encodeBytesToBytes(source, off, len);
            try {
                return new String(encoded, PREFERRED_ENCODING);
            } catch (UnsupportedEncodingException uue) {
                return new String(encoded);
            }
        }

        /**
         * Similar to {@link #encodeBytes(byte[], int, int)} but returns a byte array instead of
         * instantiating a String. This is more efficient if you're working with I/O streams and have
         * large data sets to encode.
         *
         * @param source The data to convert
         * @param off    Offset in array where conversion should begin
         * @param len    Length of data to convert
         * @return The Base64-encoded data as a String if there is an error
         * @throws NullPointerException     if source array is null
         * @throws IllegalArgumentException if source array, offset, or length are invalid
         * @since 2.3.1
         */
        public static byte[] encodeBytesToBytes(byte[] source, int off, int len) {

            if (source == null) {
                throw new NullPointerException("Cannot serialize a null array.");
            }

            if (off < 0) {
                throw new IllegalArgumentException("Cannot have negative offset: " + off);
            }

            if (len < 0) {
                throw new IllegalArgumentException("Cannot have length offset: " + len);
            }

            if (off + len > source.length) {
                throw new IllegalArgumentException(
                    String.format(
                        "Cannot have offset of %d and length of %d with array of length %d",
                        off, len, source.length));
            }

            // Bytes needed for actual encoding
            int encLen = (len / 3) * 4 + (len % 3 > 0 ? 4 : 0);

            byte[] outBuff = new byte[encLen];

            int d = 0;
            int e = 0;
            int len2 = len - 2;
            for (; d < len2; d += 3, e += 4) {
                encode3to4(source, d + off, 3, outBuff, e);
            }

            if (d < len) {
                encode3to4(source, d + off, len - d, outBuff, e);
                e += 4;
            }

            if (e <= outBuff.length - 1) {
                byte[] finalOut = new byte[e];
                System.arraycopy(outBuff, 0, finalOut, 0, e);
                return finalOut;
            } else {
                return outBuff;
            }
        }
    }

    /**
     * HTTP request exception whose cause is always an {@link IOException}
     */
    public static class HttpRequestException extends RuntimeException {

        private static final long serialVersionUID = -1170466989781746231L;

        /**
         * Create a new HttpRequestException with the given cause
         *
         * @param cause
         */
        public HttpRequestException(final IOException cause) {
            super(cause);
        }

        /**
         * Get {@link IOException} that triggered this request exception
         *
         * @return {@link IOException} cause
         */
        @Override
        public IOException getCause() {
            return (IOException) super.getCause();
        }
    }

    /**
     * Operation that handles executing a callback once complete and handling nested exceptions
     *
     * @param <V>
     */
    protected abstract static class AbstractOperation<V> implements Callable<V> {

        /**
         * Run operation
         */
        protected abstract V run() throws HttpRequestException, IOException;

        /**
         * Operation complete callback
         *
         * @throws IOException
         */
        protected abstract void done() throws IOException;

        public V call() throws HttpRequestException {
            boolean thrown = false;
            try {
                return run();
            } catch (HttpRequestException e) {
                thrown = true;
                throw e;
            } catch (IOException e) {
                thrown = true;
                throw new HttpRequestException(e);
            } finally {
                try {
                    done();
                } catch (IOException e) {
                    if (!thrown) {
                        throw new HttpRequestException(e);
                    }
                }
            }
        }
    }

    /**
     * Class that ensures a {@link Closeable} gets closed with proper exception handling.
     *
     * @param <V>
     */
    protected abstract static class AbstractCloseOperation<V> extends AbstractOperation<V> {

        private final Closeable closeable;

        private final boolean ignoreCloseExceptions;

        /**
         * Create closer for operation
         */
        protected AbstractCloseOperation(final Closeable closeable, final boolean ignoreCloseExceptions) {
            this.closeable = closeable;
            this.ignoreCloseExceptions = ignoreCloseExceptions;
        }

        @Override
        protected void done() throws IOException {
            if (closeable instanceof Flushable) {
                ((Flushable) closeable).flush();
            }
            if (ignoreCloseExceptions) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // Ignored
                }
            } else {
                closeable.close();
            }
        }
    }

    /**
     * Class that and ensures a {@link Flushable} gets flushed with proper exception handling.
     *
     * @param <V>
     */
    protected abstract static class AbstractFlushOperation<V> extends AbstractOperation<V> {

        private final Flushable flushable;

        /**
         * Create flush operation
         */
        protected AbstractFlushOperation(final Flushable flushable) {
            this.flushable = flushable;
        }

        @Override
        protected void done() throws IOException {
            flushable.flush();
        }
    }

    /**
     * Request output stream
     */
    public static class RequestOutputStream extends BufferedOutputStream {

        private final CharsetEncoder encoder;

        /**
         * Create request output stream
         */
        public RequestOutputStream(
            final OutputStream stream, final String charset, final int bufferSize) {
            super(stream, bufferSize);

            encoder = Charset.forName(getValidCharset(charset)).newEncoder();
        }

        /**
         * Write string to stream
         */
        public RequestOutputStream write(final String value) throws IOException {
            final ByteBuffer bytes = encoder.encode(CharBuffer.wrap(value));

            super.write(bytes.array(), 0, bytes.limit());

            return this;
        }
    }
}
