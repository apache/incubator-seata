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
package io.seata.core.model;

/**
 * Generic return result class
 *
 * @author zjinlei
 */
public class Result<T> {

    private T result;

    private String errMsg;

    private Object[] errMsgParams;

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public Object[] getErrMsgParams() {
        return errMsgParams;
    }

    public void setErrMsgParams(Object[] errMsgParams) {
        this.errMsgParams = errMsgParams;
    }

    public Result(T result, String errMsg, Object[] errMsgParams) {
        this.result = result;
        this.errMsg = errMsg;
        this.errMsgParams = errMsgParams;
    }

    public static Result<Boolean> ok() {
        return new Result<>(true, null, null);
    }

    public static <T> Result<T> build(T result) {
        return new Result(result, null, null);
    }

    public static <T> Result<T> build(T result, String errMsg) {
        return new Result(result, errMsg, null);
    }

    public static <T> Result<T> buildWithParams(T result, String errMsg, Object... args) {
        return new Result(result, errMsg, args);
    }
}
