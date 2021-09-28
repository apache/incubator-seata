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
package io.seata.discovery.registry;

import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.config.ConfigurationKeys;

import java.util.HashMap;
import java.util.Map;

/**
 * use url to config tne register center
 *
 * @author liujian
 */
public class RegistryURL {

    /**
     * register url
     */
    private String url;
    /**
     * register type
     */
    private String protocol;
    /**
     * serverAddr
     */
    private String host;
    private int port;
    private String path;
    /**
     * register center paramters
     */
    private Map<String, String> parameters = new HashMap<>();

    private static RegistryURL instance;

    public static RegistryURL getInstance() {
        if (instance == null) {
            synchronized (RegistryURL.class) {
                if (StringUtils.equals(getRegistryTypeKey(), ConfigurationKeys.URL)) {
                    String url = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(getRegistryUrlKey());
                    instance = new RegistryURL(url);
                }
            }
        }
        return instance;
    }

    public RegistryURL(String url) {
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

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return this.parameters.get("username");
    }

    public String getPassword() {
        return this.parameters.get("password");
    }

    public String getWeight() {
        return this.parameters.get("weight");
    }

    public String getApplication() {
        return this.parameters.get("application");
    }

    public String getCLusterName() {
        return this.parameters.get("cluster");
    }

    public String getAclToken() {
        return this.parameters.get("aclToken");
    }

    public String getKey() {
        return this.parameters.get("key");
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public static String getRegistryUrlKey() {
        return ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.URL;
    }

    public static String getRegistryTypeKey() {
        return ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE;
    }

}