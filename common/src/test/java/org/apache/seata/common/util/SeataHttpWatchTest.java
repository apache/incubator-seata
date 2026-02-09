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

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.metadata.ClusterWatchEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SeataHttpWatchTest {

    private OkHttpClient mockClient;
    private Call mockCall;
    private Response mockResponse;
    private ResponseBody mockResponseBody;
    private BufferedSource mockSource;

    @BeforeEach
    public void setUp() {
        mockClient = mock(OkHttpClient.class);
        mockCall = mock(Call.class);
        mockResponse = mock(Response.class);
        mockResponseBody = mock(ResponseBody.class);
        mockSource = mock(BufferedSource.class);
    }

    @AfterEach
    public void tearDown() {
        // Clean up if needed
    }

    @Test
    public void testCreateWatch_WithSuccessfulResponse() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);

        // Execute
        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Verify
        assertNotNull(watch);
        verify(mockClient).newCall(request);
        verify(mockCall).execute();
    }

    @Test
    public void testCreateWatch_WithUnsuccessfulResponse() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(404);
        when(mockResponse.message()).thenReturn("Not Found");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn("Error message");

        // Execute & Verify
        FrameworkException exception = assertThrows(FrameworkException.class, () -> {
            SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);
        });

        assertTrue(exception.getMessage().contains("404"));
        assertTrue(exception.getMessage().contains("Error message"));
    }

    @Test
    public void testCreateWatch_WithUnsuccessfulResponse_IOExceptionOnBodyRead() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(500);
        when(mockResponse.message()).thenReturn("Internal Server Error");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenThrow(new IOException("Read error"));

        // Execute & Verify
        FrameworkException exception = assertThrows(FrameworkException.class, () -> {
            SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);
        });

        assertTrue(exception.getMessage().contains("Watch request failed"));
    }

    @Test
    public void testCreateWatch_WithNullContentType() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn(null);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);

        // Execute
        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Verify - should still create watch but log warning
        assertNotNull(watch);
    }

    @Test
    public void testHasNext_WithAvailableData() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);
        when(mockSource.exhausted()).thenReturn(false);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        boolean hasNext = watch.hasNext();

        // Verify
        assertTrue(hasNext);
        verify(mockSource).exhausted();
    }

    @Test
    public void testHasNext_WithNoMoreData() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);
        when(mockSource.exhausted()).thenReturn(true);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        boolean hasNext = watch.hasNext();

        // Verify
        assertFalse(hasNext);
    }

    @Test
    public void testHasNext_WithIOException() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);
        when(mockSource.exhausted()).thenThrow(new IOException("Stream error"));

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        boolean hasNext = watch.hasNext();

        // Verify - should return false on IOException
        assertFalse(hasNext);
    }

    @Test
    public void testNext_WithKeepaliveEvent() throws IOException {
        // Setup - new format: group, timestamp, metadata
        String sseData =
                "CW:{\"group\":\"default-test\",\"timestamp\":1234567890,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":1}}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        // Verify
        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type);
        assertNotNull(response.object);
        assertEquals("default-test", response.object.getGroup());
        assertEquals(1234567890L, response.object.getTimestamp());
        assertNotNull(response.object.getMetadata());
        assertEquals(1L, response.object.getMetadata().getTerm());
    }

    @Test
    public void testNext_WithClusterUpdateEvent() throws IOException {
        // Setup - new format: group, timestamp, metadata
        String sseData =
                "CW:{\"group\":\"default-test\",\"timestamp\":1234567890,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":2}}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        // Verify
        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type);
        assertNotNull(response.object);
        assertEquals("default-test", response.object.getGroup());
        assertEquals(1234567890L, response.object.getTimestamp());
        assertNotNull(response.object.getMetadata());
        assertEquals(2L, response.object.getMetadata().getTerm());
    }

    @Test
    public void testNext_WithTimeoutEvent() throws IOException {
        // Setup - new format: group, timestamp, metadata (timeout events are no longer used, but test for
        // compatibility)
        String sseData =
                "CW:{\"group\":\"default-test\",\"timestamp\":1234567890,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":1}}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        // Verify - all events are now UPDATE type
        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type);
        assertNotNull(response.object);
        assertEquals("default-test", response.object.getGroup());
    }

    @Test
    public void testNext_WithUnknownEventType() throws IOException {
        // Setup - new format doesn't have type field, so this test is for backward compatibility
        String sseData =
                "CW:{\"group\":\"default-test\",\"timestamp\":1234567890,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":1}}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        // Verify - all successful events are UPDATE type
        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type);
    }

    @Test
    public void testNext_WithNullEventType() throws IOException {
        // Setup - new format: group, timestamp, metadata (metadata can be null)
        String sseData = "CW:{\"group\":\"default-test\",\"timestamp\":1234567890,\"metadata\":null}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        // Verify - all successful events are UPDATE type
        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type);
        assertNull(response.object.getMetadata());
    }

    @Test
    public void testNext_WithInvalidJson() throws IOException {
        // Setup
        String sseData = "CW:{invalid json}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        // Verify - should return ERROR type with null object
        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.ERROR, response.type);
        assertNull(response.object);
    }

    // Each event is sent in a single line format: "{prefix}{json}\n" where prefix is defined in
    // Constants.WATCH_EVENT_PREFIX, and the client reads one line at a time
    @Test
    public void testNext_WithMultipleEvents() throws IOException {
        // Simulate two events - new format: group, timestamp, metadata
        String sseData =
                "CW:{\"group\":\"default-test\",\"timestamp\":1234567890,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":1}}\n"
                        + "CW:{\"group\":\"default-test\",\"timestamp\":1234567891,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":2}}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute - read first event
        SeataHttpWatch.Response<ClusterWatchEvent> response1 = watch.next();
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response1.type);
        assertEquals(1L, response1.object.getMetadata().getTerm());

        // Execute - read second event
        SeataHttpWatch.Response<ClusterWatchEvent> response2 = watch.next();
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response2.type);
        assertEquals(2L, response2.object.getMetadata().getTerm());
    }

    @Test
    public void testNext_WithEmptyLines() throws IOException {
        // Setup - new format: group, timestamp, metadata
        String sseData =
                "CW:{\"group\":\"default-test\",\"timestamp\":1234567890,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":1}}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        // Verify - should parse the event
        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type);
    }

    @Test
    public void testNext_WithInvalidPrefix() throws IOException {
        // Setup - line without expected prefix should throw exception
        String sseData = "event: custom-event\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute and verify - should throw exception for invalid prefix
        RuntimeException exception = assertThrows(RuntimeException.class, () -> watch.next());
        assertTrue(exception.getMessage().contains("Invalid event format"));
    }

    @Test
    public void testNext_WithStreamClosedUnexpectedly() throws IOException {
        // Setup
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);
        when(mockSource.readUtf8Line()).thenReturn(null); // Stream closed

        SeataHttpWatch<ClusterWatchEvent> watch = SeataHttpWatch.createWatch(
                mockClient,
                new Request.Builder().url("http://localhost:8080/test").get().build(),
                ClusterWatchEvent.class);

        // Execute & Verify
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            watch.next();
        });

        assertTrue(exception.getMessage().contains("Stream closed unexpectedly"));
    }

    @Test
    public void testNext_WithPartialDataOnStreamClose() throws IOException {
        // Setup - stream closes with partial data
        Buffer buffer = new Buffer();
        buffer.writeString("CW:{\"group\":\"default-test\",\"timestamp\":1234567890", StandardCharsets.UTF_8);
        // Simulate stream closing before complete event

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute - should try to parse partial data
        // This will likely result in an error response due to incomplete JSON
        SeataHttpWatch.Response<ClusterWatchEvent> response = watch.next();

        // Verify - should return ERROR type due to invalid JSON
        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.ERROR, response.type);
    }

    @Test
    public void testNext_WithIOException() throws IOException {
        // Setup
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);
        when(mockSource.readUtf8Line()).thenThrow(new IOException("Read error"));

        SeataHttpWatch<ClusterWatchEvent> watch = SeataHttpWatch.createWatch(
                mockClient,
                new Request.Builder().url("http://localhost:8080/test").get().build(),
                ClusterWatchEvent.class);

        // Execute & Verify
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            watch.next();
        });

        assertTrue(exception.getMessage().contains("IO Exception"));
    }

    @Test
    public void testIterator() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        java.util.Iterator<SeataHttpWatch.Response<ClusterWatchEvent>> iterator = watch.iterator();

        // Verify - should return itself
        assertNotNull(iterator);
        assertTrue(iterator == watch);
    }

    @Test
    public void testRemove() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute & Verify
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            watch.remove();
        });

        assertEquals("remove", exception.getMessage());
    }

    @Test
    public void testClose() throws IOException {
        // Setup
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute
        watch.close();

        // Verify
        verify(mockCall).cancel();
        verify(mockResponseBody).close();
    }

    @Test
    public void testClose_WithNullCall() throws IOException {
        // Setup - create watch with null call (using reflection or a test helper)
        // For this test, we'll verify that close handles null gracefully
        // Since the constructor is private, we'll test through the public API
        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute - close should not throw exception even if called multiple times
        watch.close();
        watch.close(); // Should be safe to call multiple times
    }

    @Test
    public void testResponse_Constructor() {
        // Test Response inner class constructor
        ClusterWatchEvent event = new ClusterWatchEvent();
        event.setGroup("test-group");
        event.setTimestamp(1234567890L);

        SeataHttpWatch.Response<ClusterWatchEvent> response =
                new SeataHttpWatch.Response<>(SeataHttpWatch.Response.Type.UPDATE, event);

        assertNotNull(response);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response.type);
        assertEquals(event, response.object);
    }

    @Test
    public void testResponse_TypeEnum() {
        // Test Response.Type enum values - simplified to only UPDATE and ERROR
        SeataHttpWatch.Response.Type[] types = SeataHttpWatch.Response.Type.values();
        assertEquals(2, types.length);

        assertTrue(contains(types, SeataHttpWatch.Response.Type.UPDATE));
        assertTrue(contains(types, SeataHttpWatch.Response.Type.ERROR));
    }

    @Test
    public void testCreateWatch_WithCall() throws IOException {
        // Test createWatch with Call directly
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(mockSource);

        SeataHttpWatch<ClusterWatchEvent> watch = SeataHttpWatch.createWatch(mockCall, ClusterWatchEvent.class);

        assertNotNull(watch);
        verify(mockCall).execute();
    }

    @Test
    public void testCreateWatch_WithCall_UnsuccessfulResponse() throws IOException {
        // Test createWatch with Call - unsuccessful response
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(400);
        when(mockResponse.message()).thenReturn("Bad Request");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn("Bad request body");

        FrameworkException exception = assertThrows(FrameworkException.class, () -> {
            SeataHttpWatch.createWatch(mockCall, ClusterWatchEvent.class);
        });

        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("Bad request body"));
    }

    @Test
    public void testNext_WithMultipleDataLines() throws IOException {
        // Setup - Test multiple events, each with prefix and JSON on a single line - new format
        String sseData =
                "CW:{\"group\":\"default-test\",\"timestamp\":1234567890,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":1}}\n"
                        + "CW:{\"group\":\"default-test\",\"timestamp\":1234567891,\"metadata\":{\"nodes\":[],\"storeMode\":\"raft\",\"term\":2}}\n";
        Buffer buffer = new Buffer();
        buffer.writeString(sseData, StandardCharsets.UTF_8);

        Request request =
                new Request.Builder().url("http://localhost:8080/test").get().build();

        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.header("Content-Type")).thenReturn("text/event-stream");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.source()).thenReturn(buffer);

        SeataHttpWatch<ClusterWatchEvent> watch =
                SeataHttpWatch.createWatch(mockClient, request, ClusterWatchEvent.class);

        // Execute - should parse first event
        SeataHttpWatch.Response<ClusterWatchEvent> response1 = watch.next();
        assertNotNull(response1);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response1.type);
        assertNotNull(response1.object);
        assertEquals(1L, response1.object.getMetadata().getTerm());

        // Execute - should parse second event
        SeataHttpWatch.Response<ClusterWatchEvent> response2 = watch.next();
        assertNotNull(response2);
        assertEquals(SeataHttpWatch.Response.Type.UPDATE, response2.type);
        assertNotNull(response2.object);
        assertEquals(2L, response2.object.getMetadata().getTerm());
    }

    private boolean contains(SeataHttpWatch.Response.Type[] types, SeataHttpWatch.Response.Type type) {
        for (SeataHttpWatch.Response.Type t : types) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }
}
