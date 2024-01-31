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



import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.seata.common.ConfigurationKeys.EXTRA_DATA_KV_CHAR;
import static org.apache.seata.common.ConfigurationKeys.EXTRA_DATA_SPLIT_CHAR;


public class JwtAuthManager {
    private String accessToken;

    private String username;

    private String password;

    public final static String PRO_USERNAME = "username";

    public final static String PRO_PASSWORD = "password";

    public final static String PRO_TOKEN = "token";

    private static JwtAuthManager instance;

    private JwtAuthManager() {
    }

    public static JwtAuthManager getInstance() {
        if (instance == null) {
            instance = new JwtAuthManager();
        }
        return instance;
    }

    public void init() {
        username = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig("security." + PRO_USERNAME);
        password = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig("security." + PRO_PASSWORD);
    }

    public String getToken() {
        return accessToken;
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

    public void refreshToken(String newToken) {
        accessToken = newToken;
    }

    public void setAccessToken(String token) {
        accessToken = token;
    }

    public static HashMap<String, String> convertToHashMap(String inputString) {
        HashMap<String, String> resultMap = new HashMap<>();
        if (StringUtils.isBlank(inputString)) {
            return resultMap;
        }
        String[] keyValuePairs = inputString.split(EXTRA_DATA_SPLIT_CHAR);
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.trim().split(EXTRA_DATA_KV_CHAR);
            if (keyValue.length == 2) {
                resultMap.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return resultMap;
    }

    public static String convertToString(HashMap<String, String> inputMap) {
        if (inputMap == null || inputMap.isEmpty()) {
            return "";
        }
        StringBuilder resultString = new StringBuilder();
        for (Map.Entry<String, String> entry : inputMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String pair = key + EXTRA_DATA_KV_CHAR + value + EXTRA_DATA_SPLIT_CHAR;
            resultString.append(pair);
        }
        if (resultString.length() > 0) {
            resultString.deleteCharAt(resultString.length() - 1);
        }
        return resultString.toString();
    }
    public static String refreshAuthData(String extraData) {
        HashMap<String,String> extraDataMap = convertToHashMap(extraData);
        extraDataMap.remove(PRO_TOKEN);
        if(null != getInstance().getToken()){
            extraDataMap.put(PRO_TOKEN,getInstance().getToken());
        }else if(null!= getInstance().getUsername() && null != getInstance().getPassword()){
            extraDataMap.put(PRO_USERNAME,getInstance().getUsername());
            extraDataMap.put(PRO_PASSWORD,getInstance().getPassword());
        }
        return convertToString(extraDataMap);
    }


}
