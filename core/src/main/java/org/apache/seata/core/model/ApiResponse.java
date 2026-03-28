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
package org.apache.seata.core.model;

/**
 * api response
 */
public class ApiResponse<T> {

    private static final String SUCCESS = "success";
    private static final String FAIL = "fail";

    /**
     * Status code
     *
     */
    private int code;

    /**
     * Message
     *
     */
    private String message;

    /**
     * Response data
     *
     */
    private T data;

    /**
     * Timestamp
     *
     */
    private Long timestamp;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResponse<T> of(int code, String message) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setCode(code);
        apiResponse.setMessage(message);
        return apiResponse;
    }

    public static <T> ApiResponse<T> of(int code, String message, T data) {
        ApiResponse<T> apiResponse = new ApiResponse<>();
        apiResponse.setCode(code);
        apiResponse.setMessage(message);
        apiResponse.setData(data);
        return apiResponse;
    }

    public static <T> ApiResponse<T> success() {
        return of(0, SUCCESS);
    }

    public static <T> ApiResponse<T> success(T data) {
        return of(0, SUCCESS, data);
    }

    public static <T> ApiResponse<T> fail() {
        return of(-1, FAIL);
    }

    public static <T> ApiResponse<T> fail(T data) {
        return of(-1, FAIL, data);
    }
}
