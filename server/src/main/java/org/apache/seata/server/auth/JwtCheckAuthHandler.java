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
package org.apache.seata.server.auth;


import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.auth.AuthResult;
import org.apache.seata.core.auth.AuthResultBuilder;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.server.auth.utils.ManagerRegJwtTokenUtils;

import java.util.HashMap;


@LoadLevel(name = "jwtCheckAuthHandler", order = 1)
public class JwtCheckAuthHandler extends AbstractCheckAuthHandler {

    private static final String PRO_TOKEN = "token";

    private static final String PRO_REFRESH_TOKEN = "refresh_token";

    private static final String PRO_USERNAME = "username";

    private static final String PRO_PASSWORD = "password";

    private static final ManagerRegJwtTokenUtils jwtTokenUtils = new ManagerRegJwtTokenUtils();

    @Override
    public AuthResult doRegTransactionManagerCheck(RegisterTMRequest request) {
        return checkAuthData(request.getExtraData());
    }

    @Override
    public AuthResult doRegResourceManagerCheck(RegisterRMRequest request) {
        return checkAuthData(request.getExtraData());
    }

    private AuthResult checkAuthData(String extraData) {
        HashMap<String, String> extraDataMap = StringUtils.string2Map(extraData);
        // 1.check username/password
        String username = extraDataMap.get(PRO_USERNAME);
        String password = extraDataMap.get(PRO_PASSWORD);
        String accessToken = extraDataMap.get(PRO_TOKEN);
        String refreshToken = extraDataMap.get(PRO_REFRESH_TOKEN);
        if (username != null && password != null) {
            return jwtTokenUtils.checkUsernamePassword(username, password);
        } else if (accessToken != null) {
            // 2.check token
            return jwtTokenUtils.checkAccessToken(accessToken);
        } else if (refreshToken != null) {
            return jwtTokenUtils.checkRefreshToken(refreshToken);
        }
        return new AuthResultBuilder().setResultCode(ResultCode.Failed).build();
    }

    @Override
    public String fetchNewToken(AuthResult authResult) {
        if (authResult != null && authResult.getResultCode().equals(ResultCode.Success)) {
            HashMap<String, String> extraDataMap = new HashMap<>();
            if (authResult.getAccessToken() != null) {
                extraDataMap.put(PRO_TOKEN, authResult.getAccessToken());
            }
            if (authResult.getRefreshToken() != null) {
                extraDataMap.put(PRO_REFRESH_TOKEN, authResult.getRefreshToken());
            }
            return StringUtils.map2String(extraDataMap);
        }
        return null;
    }
}
