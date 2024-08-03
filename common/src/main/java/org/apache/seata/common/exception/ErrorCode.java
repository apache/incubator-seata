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
 * The enum Error code.
 */
public enum ErrorCode {

    /**
     * 0001 ~ 0099  Configuration related errors
     */
    ERR_CONFIG(ErrorType.Config, 0001),

    /**
     * 0100 ~ 0199 Security related errors
     */
    ERR_DESERIALIZATION_SECURITY(ErrorType.Security, 0156),

    /**
     * The error code of the transaction exception.
     */


    /**
     * The error code of the sql exception
     */
    ERROR_SQL(ErrorType.Datasource, 0201);

    private int code;
    private ErrorType type;

    ErrorCode(ErrorType type, int code) {
        this.code = code;
        this.type = type;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() {
        return type.name();
    }

    /**
     * Gets message.
     *
     * @param params the params
     * @return the message
     */
    public String getMessage(String... params) {
        return ResourceBundleUtil.getInstance().getMessage(this.name(), this.getCode(), this.getType(), params);
    }

    /**
     * The enum Error type.
     */
    enum ErrorType {
        /**
         * Config error type.
         */
        Config,
        /**
         * Network error type.
         */
        Network,
        /**
         * Tm error type.
         */
        TM,
        /**
         * Rm error type.
         */
        RM,
        /**
         * Tc error type.
         */
        TC,
        /**
         * Datasource error type.
         */
        Datasource,
        /**
         * Security error type.
         */
        Security,
        /**
         * Other error type.
         */
        Other;
    }

}
