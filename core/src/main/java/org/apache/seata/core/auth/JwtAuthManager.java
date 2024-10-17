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


import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;

import java.util.HashMap;

public class JwtAuthManager {
    private String refreshToken;

    private String accessToken;

    private boolean isAccessTokenNearExpiration;

    private String username;

    private String password;

    public final static String PRO_USERNAME = "username";

    public final static String PRO_PASSWORD = "password";

    public final static String PRO_TOKEN = "token";

    public final static String PRO_REFRESH_TOKEN = "refresh_token";

    private static volatile JwtAuthManager instance;

    private JwtAuthManager() {
    }

    public static JwtAuthManager getInstance() {
        if (instance == null) {
            synchronized (JwtAuthManager.class) {
                if (instance == null) {
                    instance = new JwtAuthManager();
                    instance.username = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.SECURITY_USERNME);
                    instance.password = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.SECURITY_PASSWORD);
                    instance.isAccessTokenNearExpiration = false;
                }
            }
        }
        return instance;
    }

    public void init() {
        }

    public boolean isAccessTokenNearExpiration() {
        return isAccessTokenNearExpiration;
    }

    public void setAccessTokenNearExpiration(boolean accessTokenNearExpiration) {
        isAccessTokenNearExpiration = accessTokenNearExpiration;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void refreshToken(String newAccessToken, String newRefreshToken) {
        if (newAccessToken != null) {
            accessToken = newAccessToken;
            isAccessTokenNearExpiration = false;
        }
        if (newRefreshToken != null) {
            refreshToken = newRefreshToken;
        }
    }

    public void setAccessToken(String token) {
        accessToken = token;
    }

    public void setRefreshToken(String token) {
        refreshToken = token;
    }

    public String getAuthData() {
        HashMap<String, String> extraDataMap = new HashMap<>();
        extraDataMap.remove(PRO_TOKEN);
        if (accessToken != null && !isAccessTokenNearExpiration) {
            extraDataMap.put(PRO_TOKEN, accessToken);
        } else if (refreshToken != null) {
            extraDataMap.put(PRO_REFRESH_TOKEN, refreshToken);
        } else if (username != null && password != null) {
            extraDataMap.put(PRO_USERNAME, username);
            extraDataMap.put(PRO_PASSWORD, password);
        }
        return StringUtils.map2String(extraDataMap);
    }

}
