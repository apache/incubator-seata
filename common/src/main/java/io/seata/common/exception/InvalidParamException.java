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
package io.seata.common.exception;

/**
 * The type Invalid param exception.
 *
 * @author miaoxueyu
 */
public class InvalidParamException extends RuntimeException {

    /**
     * Instantiates a new Invalid param exception.
     */
    public InvalidParamException() {
        super();
    }

    /**
     * Instantiates a new Invalid param exception.
     *
     * @param message the message
     */
    public InvalidParamException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Invalid param exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public InvalidParamException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Invalid param exception.
     *
     * @param cause the cause
     */
    public InvalidParamException(Throwable cause) {
        super(cause);
    }
}
