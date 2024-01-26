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
 * The redis operate exception
 *
 */
public class RedisException extends FrameworkException {

    /**
     * Instantiates a new Redis exception.
     *
     * @param err the err
     */
    public RedisException(FrameworkErrorCode err) {
        super(err);
    }

    /**
     * Instantiates a new Redis exception.
     *
     * @param msg the msg
     */
    public RedisException(String msg) {
        super(msg);
    }

    /**
     * Instantiates a new Redis exception.
     *
     * @param msg     the msg
     * @param errCode the err code
     */
    public RedisException(String msg, FrameworkErrorCode errCode) {
        super(msg, errCode);
    }

    /**
     * Instantiates a new Redis exception.
     *
     * @param cause   the cause
     * @param msg     the msg
     * @param errCode the err code
     */
    public RedisException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(cause, msg, errCode);
    }

    /**
     * Instantiates a new Redis exception.
     *
     * @param th the th
     */
    public RedisException(Throwable th) {
        super(th);
    }

    /**
     * Instantiates a new Redis exception.
     *
     * @param th  the th
     * @param msg the msg
     */
    public RedisException(Throwable th, String msg) {
        super(th, msg);
    }
}
