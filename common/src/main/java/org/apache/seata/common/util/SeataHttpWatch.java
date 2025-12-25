/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.common.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.apache.seata.common.exception.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 * Seata HTTP/2 Watch implementation.
 * Consumes server-pushed SSE data frames via an iterator-style API.
 *
 * @param <T> event data type
 */
public class SeataHttpWatch<T>
        implements Iterator<SeataHttpWatch.Response<T>>, Iterable<SeataHttpWatch.Response<T>>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataHttpWatch.class);

    private final ResponseBody responseBody;
    private final BufferedSource source;
    private final Call call;
    private final ObjectMapper objectMapper;
    private final Class<T> eventType;

    /**
     * Response wrapper containing event type and data
     */
    public static class Response<T> {
        /**
         * Event type enum - matches SSE event field values
         */
        public enum Type {
            /**
             * Cluster update event
             */
            CLUSTER_UPDATE,
            /**
             * Keep-alive event (connection established)
             */
            KEEPALIVE,
            /**
             * Timeout event (stream closed due to timeout)
             */
            TIMEOUT,
            /**
             * Error event
             */
            ERROR
        }

        public final Type type;

        public final T object;

        public Response(Type type, T object) {
            this.type = type;
            this.object = object;
        }
    }

    /**
     * Create a Watch instance from an OkHttp call
     *
     * @param call      the prepared HTTP call
     * @param eventType the class type for deserializing event data
     * @param <T>       the event data type
     * @return a Watch instance
     * @throws IOException if the request fails
     */
    public static <T> SeataHttpWatch<T> createWatch(Call call, Class<T> eventType) throws IOException {

        okhttp3.Response response = call.execute();

        if (!response.isSuccessful()) {
            String respBody = null;
            try (ResponseBody body = response.body()) {
                if (body != null) {
                    respBody = body.string();
                }
            } catch (IOException e) {
                throw new FrameworkException(e, "Watch request failed: " + response.message());
            }
            throw new FrameworkException(
                    String.format("Watch request failed with code %d: %s", response.code(), respBody));
        }

        // Verify Content-Type is SSE
        String contentType = response.header("Content-Type");
        if (contentType == null || !contentType.contains("text/event-stream")) {
            LOGGER.warn("Expected Content-Type: text/event-stream, got: {}", contentType);
        }

        return new SeataHttpWatch<>(response.body(), call, eventType);
    }

    /**
     * Create a Watch instance with a prepared request
     *
     * @param client    the OkHttpClient instance
     * @param request   the HTTP request
     * @param eventType the class type for deserializing event data
     * @param <T>       the event data type
     * @return a Watch instance
     * @throws IOException if the request fails
     */
    public static <T> SeataHttpWatch<T> createWatch(OkHttpClient client, Request request, Class<T> eventType)
            throws IOException {

        Call call = client.newCall(request);
        return createWatch(call, eventType);
    }

    private SeataHttpWatch(ResponseBody responseBody, Call call, Class<T> eventType) {
        this.responseBody = responseBody;
        this.source = responseBody.source();
        this.call = call;
        this.objectMapper = new ObjectMapper();
        this.eventType = eventType;
    }

    @Override
    public boolean hasNext() {
        try {
            // Check if source is exhausted (stream closed)
            return !source.exhausted();
        } catch (IOException e) {
            LOGGER.error("Error checking if stream has more data", e);
            return false;
        }
    }

    @Override
    public Response<T> next() {
        try {
            // Read complete SSE event in a loop (not recursive to avoid stack overflow)
            // SSE format: "data: {json}\n\n"
            // Each event is separated by double newline
            // Event type is included in the JSON data, not in a separate "event:" field

            StringBuilder dataBuffer = new StringBuilder();

            // Read lines until we get a complete event (ending with empty line)
            // This loop reads all lines of a single event atomically
            while (true) {
                String line = source.readUtf8Line();

                if (line == null) {
                    // Stream closed
                    if (dataBuffer.length() > 0) {
                        // We have partial data, try to parse it
                        String eventJson = dataBuffer.toString();
                        return parseEvent(eventJson);
                    }
                    throw new RuntimeException("Stream closed unexpectedly");
                }

                if (line.startsWith("data: ")) {
                    String jsonData = line.substring(6);
                    if (dataBuffer.length() > 0) {
                        dataBuffer.append('\n');
                    }
                    dataBuffer.append(jsonData);
                } else if (line.isEmpty()) {
                    // Empty line indicates end of event
                    if (dataBuffer.length() > 0) {
                        String eventJson = dataBuffer.toString();
                        return parseEvent(eventJson);
                    }
                    // Empty line but no data, continue reading (skip blank lines between events)
                } else {
                    LOGGER.debug("Unknown SSE line format, ignoring: {}", line);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("IO Exception during next()", e);
        }
    }

    /**
     * Parse SSE event JSON into Response object
     *
     * @param json the JSON string to parse (contains type field)
     * @return the parsed Response object
     * @throws IOException if parsing fails
     */
    private Response<T> parseEvent(String json) throws IOException {
        try {
            // Parse JSON once to get both event type and full data
            // First, parse to extract event type
            EventMetadata metadata = objectMapper.readValue(json, EventMetadata.class);

            // Map event type from JSON to Response.Type
            Response.Type responseType = mapEventTypeToResponseType(metadata.type);

            // Deserialize the full event data (parse once, reuse if possible)
            T eventData = objectMapper.readValue(json, eventType);

            return new Response<>(responseType, eventData);

        } catch (Exception e) {
            LOGGER.error("Failed to parse event JSON: {}", json, e);
            // Return error response
            return new Response<>(Response.Type.ERROR, null);
        }
    }

    /**
     * Map event type from JSON to Response.Type
     *
     * @param eventType the event type from JSON "type" field
     * @return the corresponding Response.Type
     */
    private Response.Type mapEventTypeToResponseType(String eventType) {
        if (eventType == null) {
            return Response.Type.CLUSTER_UPDATE; // Default
        }
        switch (eventType) {
            case "cluster-update":
                return Response.Type.CLUSTER_UPDATE;
            case "keepalive":
                return Response.Type.KEEPALIVE;
            case "timeout":
                return Response.Type.TIMEOUT;
            default:
                LOGGER.warn("Unknown event type: {}, defaulting to CLUSTER_UPDATE", eventType);
                return Response.Type.CLUSTER_UPDATE;
        }
    }

    /**
     * Event metadata for parsing event type from JSON
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class EventMetadata {
        /**
         * Event type string
         */
        public String type;
    }

    @Override
    public Iterator<Response<T>> iterator() {
        return this;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public void close() throws IOException {
        if (call != null) {
            call.cancel();
        }
        if (responseBody != null) {
            responseBody.close();
        }
    }
}
