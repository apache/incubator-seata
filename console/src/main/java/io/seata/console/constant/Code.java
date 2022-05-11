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
package io.seata.console.constant;

/**
 * The Code for the response of message
 *
 * @author jameslcj
 */
public enum Code {
    /**
     * response success
     */
    SUCCESS("200", "ok"),
    /**
     * server error
     */
    ERROR("500", "Server error"),
    /**
     * the custom error
     */
    LOGIN_FAILED("401", "Login failed");

    /**
     * The Code.
     */
    public String code;

    /**
     * The Msg.
     */
    public String msg;

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

