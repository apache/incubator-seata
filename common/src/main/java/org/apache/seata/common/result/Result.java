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

import java.io.Serializable;

/**
 * The basic result
 */
public class Result implements Serializable {
    private static final long serialVersionUID = 7761261124298767L;

    private final String code;
    private final String message;

    public Result() {
        this(null, null);
    }

    public Result(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isSuccess() {
        return this.code.equals(SUCCESS.code);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
