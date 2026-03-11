/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.common.exception;

/**
 * The type Not support yet exception.
 */
public class NotSupportYetException extends RuntimeException {

    /**
     * Instantiates a new Not support yet exception.
     */
    public NotSupportYetException() {
        super();
    }

    /**
     * Instantiates a new Not support yet exception.
     *
     * @param message the message
     */
    public NotSupportYetException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Not support yet exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public NotSupportYetException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Not support yet exception.
     *
     * @param cause the cause
     */
    public NotSupportYetException(Throwable cause) {
        super(cause);
    }
}
