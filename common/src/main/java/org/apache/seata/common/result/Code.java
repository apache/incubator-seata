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
package org.apache.seata.common.result;

/**
 * The Code for the response of message
 *
 */
public enum Code {
    /**
     * response success
     */
    SUCCESS("200", "ok"),
    /**
     * the custom error
     */
    ACCESS_TOKEN_NEAR_EXPIRATION("200", "Access token is near expiration"),
    /**
     * server error
     */
    ERROR("500", "Server error"),
    /**
     * the custom error
     */
    LOGIN_FAILED("401", "Login failed"),
    /**
     * the custom error
     */
    CHECK_TOKEN_FAILED("401", "Check token failed"),
    /**
     * the custom error
     */
    ACCESS_TOKEN_EXPIRED("401", "Access token expired"),
    /**
     * the custom error
     */
    REFRESH_TOKEN_EXPIRED("401", "Refresh token expired");

    /**
     * The Code.
     */
    private String code;

    /**
     * The Msg.
     */
    private String msg;

    private Code(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Sets code.
     *
     * @param code the code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets msg.
     *
     * @return the msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Sets msg.
     *
     * @param msg the msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * Gets error msg.
     *
     * @param code the code
     * @return the error msg
     */
    public static String getErrorMsg(String code) {
        Code[] errorCodes = values();
        for (Code errCode : errorCodes) {
            if (errCode.getCode().equals(code)) {
                return errCode.getMsg();
            }
        }
        return null;
    }
}

