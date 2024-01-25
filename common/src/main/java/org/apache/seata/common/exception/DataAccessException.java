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
 * the data access exception
 */
public class DataAccessException extends StoreException {

    /**
     * constructor with framework error code
     * @param err the framework error code
     */
    public DataAccessException(FrameworkErrorCode err) {
        super(err);
    }

    /**
     * constructor with msg
     * @param msg the msg
     */
    public DataAccessException(String msg) {
        super(msg);
    }

    /**
     * constructor with cause
     * @param cause the cause
     */
    public DataAccessException(Throwable cause) {
        super(cause);
    }

    /**
     * constructor with msg and framework error code
     * @param msg the msg
     * @param errCode the framework error code
     */
    public DataAccessException(String msg, FrameworkErrorCode errCode) {
        super(msg, errCode);
    }

    /**
     * constructor with cause and msg and framework error code
     * @param cause the throwable
     * @param msg the msg
     * @param errCode the framework error code
     */
    public DataAccessException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(cause, msg, errCode);
    }


}
