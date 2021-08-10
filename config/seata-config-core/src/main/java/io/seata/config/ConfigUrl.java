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
package io.seata.config;

import io.seata.common.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * use url to config tne configuration center
 *
 * @author liujian
 */
public class ConfigUrl {

    /**
     * config url
     */
    private String url;
    /**
     * config type
     */
    private String protocol;
    /**
     * config serverAddr
     */
    private String host;
    private int port;
    private String path;
    /**
     * config center paramters
     */
    private Map<String, String> parameters = new HashMap<>();


    private static ConfigUrl instance;

    public static ConfigUrl getInstance() {
        if (instance == null) {
            synchronized (ConfigUrl.class) {
                if (StringUtils.equals(ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(getConfigTypeKey()), ConfigurationKeys.URL)) {
                    String url = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(getConfigUrlKey());
                    instance = new ConfigUrl(url);
                }
            }
        }
        return instance;
    }

    public ConfigUrl(String url) {
        valueOf(url);
    }

    public void valueOf(String url) {
        if (url != null && (url.trim().length() != 0)) {
            int index = url.indexOf(63);
            if (index > 0) {
                String[] params = url.substring(index + 1).split("&");
                for (String param : params) {
                    if (param.trim().length() > 0) {
                        int i = param.indexOf(61);
                        if (i >= 0) {
                            String key = param.substring(0, i);
                            String value = param.substring(i + 1);
                            parameters.put(key, value);
                        } else {
                            parameters.put(param, param);
                        }
                    }
                }
                url = url.substring(0, index);
            }
            index = url.indexOf("://");
            if (index >= 0) {
                if (index == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                }
                this.protocol = url.substring(0, index);
                url = url.substring(index + 3);
            }
            index = url.indexOf(47);
            if (index >= 0) {
                this.path = url.substring(index + 1);
                url = url.substring(0, index);
            }
            index = url.lastIndexOf(58);
            if (index >= 0 && index < url.length() - 1) {
                this.port = Integer.parseInt(url.substring(index + 1));
            }
            this.host = url;
        } else {
            throw new IllegalArgumentException("url == null");
        }
    }


    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getUsername() {
        return this.parameters.get("username");
    }

    public String getPassword() {
        return this.parameters.get("password");
    }

    public static String getConfigTypeKey() {
        return ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE;
    }

    public static String getConfigUrlKey() {
        return ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.URL;
    }

}