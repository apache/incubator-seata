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
 * The type Should never happen exception.
 *
 * @author slievrly
 */
public class ShouldNeverHappenException extends RuntimeException {

    /**
     * Instantiates a new Should never happen exception.
     */
    public ShouldNeverHappenException() {
        super();
    }

    /**
     * Instantiates a new Should never happen exception.
     *
     * @param message the message
     */
    public ShouldNeverHappenException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Should never happen exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ShouldNeverHappenException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Should never happen exception.
     *
     * @param cause the cause
     */
    public ShouldNeverHappenException(Throwable cause) {
        super(cause);
    }
}
