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
package org.apache.seata.common.executor;

import okhttp3.Response;

/**
 * The interface StreamHttpCallback for handling HTTP/2 streaming responses.
 * This callback allows processing data chunks as they arrive from the server,
 * enabling real-time handling of server-pushed data frames or data streams.
 *
 * @author Apache Seata
 */
public interface StreamHttpCallback {

    /**
     * Called when the response headers are received.
     * This is called once before any data chunks arrive.
     *
     * @param response the HTTP response containing headers and status code
     */
    void onHeaders(Response response);

    /**
     * Called when a data chunk is received from the server.
     * This method may be called multiple times as data arrives.
     *
     * @param data the data chunk received from the server
     * @param isLast true if this is the last data chunk, false otherwise
     */
    void onData(byte[] data, boolean isLast);

    /**
     * Called when the stream response is completed successfully.
     * This is called after all data chunks have been received.
     */
    void onComplete();

    /**
     * Called when an error occurs during the stream processing.
     *
     * @param e the exception that occurred
     */
    void onError(Throwable e);

    /**
     * Called when the HTTP request is cancelled.
     */
    void onCancelled();
}


