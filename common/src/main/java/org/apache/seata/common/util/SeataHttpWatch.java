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

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 * A single-use, forward-only event stream.
 * <p>This class implements both {@link Iterable} and {@link Iterator}
 * for convenience, but it supports only a single iteration.
 * Calling {@link #iterator()} multiple times or attempting
 * concurrent iterations is not supported.
 *
 * Seata HTTP/2 Watch implementation.
 * Consumes server-pushed event stream via an iterator-style API.
 * Each line contains a single event in "data: {json}" format.
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
     * Response wrapper containing event data.
     * Since event format is simplified (no type field), all successful events are treated the same.
     * Client can determine the nature of the event by comparing metadata.term.
     */
    public static class Response<T> {
        /**
         * Response type enum
         */
        public enum Type {
            /**
             * Normal event with data (connection established or cluster changed)
             */
            UPDATE,
            /**
             * Error event (parse error or connection error)
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

        // Verify Content-Type is event stream
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
            /*
            Read a single line and parse it as an event.
            Format: "{prefix}{json}\n" where prefix is defined in Constants.WATCH_EVENT_PREFIX.
            Each line is a complete event, event type is included in the JSON data.
            */
            String line = source.readUtf8Line();
            if (line == null) {
                throw new RuntimeException("Stream closed unexpectedly");
            }

            if (!line.startsWith(Constants.WATCH_EVENT_PREFIX)) {
                throw new RuntimeException("Invalid event format: expected prefix '" + Constants.WATCH_EVENT_PREFIX
                        + "', got: " + (line.length() > 20 ? line.substring(0, 20) + "..." : line));
            }

            String jsonData = line.substring(Constants.WATCH_EVENT_PREFIX.length());
            return parseEvent(jsonData);

        } catch (IOException e) {
            throw new RuntimeException("IO Exception during next()", e);
        }
    }

    /**
     * Parse event JSON into Response object.
     * Simplified format: only contains group, timestamp, and metadata fields.
     *
     * @param json the JSON string to parse
     * @return the parsed Response object
     * @throws IOException if parsing fails
     */
    private Response<T> parseEvent(String json) throws IOException {
        try {
            T eventData = objectMapper.readValue(json, eventType);
            return new Response<>(Response.Type.UPDATE, eventData);

        } catch (Exception e) {
            LOGGER.error("Failed to parse event JSON: {}", json, e);
            // Return error response
            return new Response<>(Response.Type.ERROR, null);
        }
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
