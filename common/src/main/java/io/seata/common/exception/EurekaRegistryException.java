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
 * eureka registry exception
 *
 * @author: rui_849217@163.com
 * @date: 2018/2/18
 */
public class EurekaRegistryException extends RuntimeException {
    /**
     * eureka registry exception.
     */
    public EurekaRegistryException() {
        super();
    }

    /**
     * eureka registry exception.
     *
     * @param message the message
     */
    public EurekaRegistryException(String message) {
        super(message);
    }

    /**
     * eureka registry exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public EurekaRegistryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * eureka registry exception.
     *
     * @param cause the cause
     */
    public EurekaRegistryException(Throwable cause) {
        super(cause);
    }
}
