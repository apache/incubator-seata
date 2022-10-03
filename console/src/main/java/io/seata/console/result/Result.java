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
package io.seata.console.result;

import io.seata.console.constant.Code;

import java.io.Serializable;

/**
 * The basic result
 * @author zhongxiang.wang
 */
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 7761261124298767L;


    private String code;
    private String message;

    public Result() {
    }

    public Result(Code code) {
        this.code = code.getCode();
        this.message = code.getMsg();
    }

    public Result(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static Result error() {
        return new Result(Code.ERROR);
    }

    public static Result ok() {
        return new Result(Code.SUCCESS);
    }

    public static Result result(Code code) {
        return new Result(code);
    }

    public boolean isSuccess() {
        return Code.SUCCESS.getCode().equals(this.code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
