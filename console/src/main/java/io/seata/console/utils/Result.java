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
package io.seata.console.utils;

import java.io.Serializable;

import io.seata.console.constant.Code;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * the struct of response
 *
 * @param <T> the type parameter
 * @author jameslcj
 */
@Data
@Accessors(chain = true)
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 7154887528070131284L;

    private String message;

    private Integer code;

    private Boolean success;

    private T data;

    /**
     * Of error result.
     *
     * @param errorCode the error code
     * @return the result
     */
    public static Result ofError(Code errorCode) {
        return of(errorCode.msg, errorCode.code, null, false);
    }

    /**
     * Of error result.
     *
     * @param code the code
     * @return the result
     */
    public static Result ofError(Integer code) {
        String errorMsg = Code.getErrorMsg(code);
        return of(errorMsg, code, null, false);
    }

    /**
     * Of error result.
     *
     * @param msg  the msg
     * @param code the code
     * @return the result
     */
    public static Result ofError(String msg, Integer code) {
        return of(msg, code, null, false);
    }

    /**
     * Of error result.
     *
     * @param msg the msg
     * @return the result
     */
    public static Result ofError(String msg) {
        return of(msg, Code.ERROR.code, null, false);
    }

    /**
     * Of error result.
     *
     * @param msg       the msg
     * @param sessionId the session id
     * @return the result
     */
    public static Result ofError(String msg, String sessionId) {
        return of(msg, Code.ERROR.code, null, false, sessionId);
    }

    /**
     * Of success result.
     *
     * @param <T>  the type parameter
     * @param data the data
     * @return the result
     */
    public static <T> Result<T> ofSuccess(T data) {
        return of(null, Code.SUCCESS.code, data, true);
    }

    /**
     * Of success result.
     *
     * @param <T>  the type parameter
     * @param code the code
     * @param data the data
     * @return the result
     */
    public static <T> Result<T> ofSuccess(Integer code, T data) {
        return of(null, code, data, true);
    }

    /**
     * Of result.
     *
     * @param <T>     the type parameter
     * @param msg     the msg
     * @param code    the code
     * @param data    the data
     * @param success the success
     * @return the result
     */
    public static <T> Result<T> of(String msg, Integer code, T data, Boolean success) {
        Result result = new Result();
        result.setMessage(msg).setSuccess(success).setData(data).setCode(code);
        return result;
    }

    /**
     * Of result.
     *
     * @param <T>       the type parameter
     * @param msg       the msg
     * @param code      the code
     * @param data      the data
     * @param success   the success
     * @param sessionId the session id
     * @return the result
     */
    public static <T> Result<T> of(String msg, Integer code, T data, Boolean success, String sessionId) {
        Result result = of(msg, code, data, success);
        return result;
    }
}
