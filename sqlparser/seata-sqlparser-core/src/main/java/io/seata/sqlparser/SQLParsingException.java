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
package io.seata.sqlparser;

/**
 * The type Sql parsing exception.
 *
 * @author sharajava
 */
public class SQLParsingException extends RuntimeException {
    /**
     * Instantiates a new Sql parsing exception.
     *
     * @param message the message
     */
    public SQLParsingException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Sql parsing exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public SQLParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Sql parsing exception.
     *
     * @param cause the cause
     */
    public SQLParsingException(Throwable cause) {
        super(cause);
    }
}
