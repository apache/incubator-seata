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
<<<<<<<< HEAD:console/src/main/java/org/apache/seata/console/result/SingleResult.java
package org.apache.seata.console.result;

import java.io.Serializable;

import org.apache.seata.console.constant.Code;
========
package org.apache.seata.common.result;

import java.io.Serializable;

>>>>>>>> upstream/2.x:common/src/main/java/org/apache/seata/common/result/SingleResult.java

/**
 * The single result
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
