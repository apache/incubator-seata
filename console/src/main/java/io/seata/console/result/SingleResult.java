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

import io.seata.console.constant.Code;

/**
 * The single result
 * @author zhongxiang.wang
 */
public class SingleResult<T> extends Result<T>  implements Serializable {
    private static final long serialVersionUID = 77612626624298767L;

    /**
     * the data
     */
    private T data;

    public SingleResult(String code, String message) {
        super(code, message);
    }

    public SingleResult(String code, String message, T data) {
        super(code, message);
        this.data = data;
    }

    public static <T> SingleResult<T> failure(String code, String msg) {
        return new SingleResult<>(code, msg);
    }

    public static <T> SingleResult<T> failure(Code errorCode) {
        return new SingleResult(errorCode.getCode(), errorCode.getMsg());
    }

    public static <T> SingleResult<T> success(T data) {
        return new SingleResult<>(SUCCESS_CODE, SUCCESS_MSG,data);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

}
