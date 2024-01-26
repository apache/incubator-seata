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
package org.apache.seata.common.exception;



/**
 * Exception indicating a retryable failure. This exception is typically thrown
 * when a retryable process fails, and it extends RuntimeException to
 * signal that it is an unchecked exception.
 */
public class RetryableException extends Exception {

    /**
     * Constructs a new RetryableException with no detailed message.
     */
    public RetryableException() {
        super();
    }

    /**
     * Constructs a new RetryableException with the specified detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the getMessage() method).
     */
    public RetryableException(String message) {
        super(message);
    }

    /**
     * Constructs a new RetryableException with the specified cause.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              getCause() method).
     */
    public RetryableException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new RetryableException with the specified detail
     * message and cause.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the getMessage() method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                getCause() method).
     */
    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}

