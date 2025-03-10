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

import static org.apache.seata.common.result.Code.SUCCESS;
import static org.apache.seata.common.result.Code.INTERNAL_SERVER_ERROR;

/**
 * The single result
 */
public class SingleResult<T> extends Result {
    private static final long serialVersionUID = 77612626624298767L;

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
        return failure(errorCode.code, errorCode.msg);
    }

    public static <T> SingleResult<T> failure(String msg) {
        return failure(INTERNAL_SERVER_ERROR.code, msg);
    }

    public static <T> SingleResult<T> success(String msg, T data) {
        return new SingleResult<>(SUCCESS.code, msg, data);
    }

    public static SingleResult<Void> success(String msg) {
        return success(msg, null);
    }

    public static <T> SingleResult<T> success() {
        return success(SUCCESS.msg, null);
    }

    public static <T> SingleResult<T> successWithData(T data) {
        return success(SUCCESS.msg, data);
    }

    public T getData() {
        return data;
    }

    public void setData(final T data) {
        this.data = data;
    }
}
