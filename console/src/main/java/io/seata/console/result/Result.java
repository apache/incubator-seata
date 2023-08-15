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

import java.io.Serializable;

/**
 * The basic result
 * @author zhongxiang.wang
 */
public class Result<T>  implements Serializable {
    private static final long serialVersionUID = 7761261124298767L;

    public static final String SUCCESS_CODE = "200";
    public static final String SUCCESS_MSG = "success";

    private String code = SUCCESS_CODE;
    private String message = SUCCESS_MSG;

    public Result() {
    }

    public Result(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(this.code);
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
