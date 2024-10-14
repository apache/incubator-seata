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
package org.apache.seata.core.auth;

import org.apache.seata.core.protocol.ResultCode;

public class AuthResultBuilder {
    private ResultCode resultCode;
    private String accessToken;
    private String refreshToken;

    public ResultCode getResultCode() {
        return resultCode;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    // set resultCode
    public AuthResultBuilder setResultCode(ResultCode resultCode) {
        this.resultCode = resultCode;
        return this;
    }

    // set accessToken
    public AuthResultBuilder setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    // set refreshToken
    public AuthResultBuilder setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    // build AuthResult
    public AuthResult build() {
        return new AuthResult(this);
    }
}
