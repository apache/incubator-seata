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
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * use url to config tne configuration center
 *
 * @author liujian
 */
public class ConfigurationUrl {

    /**
     * config serverAddr
     */
    private String serverAddr;
    private String protocol;
    private String path;
    /**
     * config center paramters
     */
    private Map<String, String> parameters = new HashMap<>();

    private static volatile ConfigurationUrl instance;

    public static ConfigurationUrl getInstance(Configuration configuration) {
        if (instance == null) {
            synchronized (ConfigurationUrl.class) {
                if (StringUtils.isNotBlank(configuration.getConfig(getConfigUrlKey()))) {
                    if (instance == null) {
                        String url = configuration.getConfig(getConfigUrlKey());
                        instance = new ConfigurationUrl(url);
                    }
                }
            }
        }
        return instance;
    }

    public ConfigurationUrl(String url) {
        valueOf(url);
    }

    public static Configuration proxy(Configuration originalConfiguration) {
        return (Configuration)Enhancer
            .create(Configuration.class, (MethodInterceptor)(proxy, method, args, methodProxy) -> {
                if (method.getName().startsWith(ConfigurationKeys.METHOD_PREFIX) && !method.getName()
                    .equalsIgnoreCase(ConfigurationKeys.METHOD_LATEST_CONFIG)) {
                    String rawDataId = (String)args[0];
                    String url = originalConfiguration.getConfig(getConfigUrlKey());
                    if (StringUtils.isNotBlank(url)) {
                        getInstance(originalConfiguration);
                        String[] subDataId = rawDataId.split("\\.");
                        int len = subDataId.length;
                        return instance.getConfig(subDataId[len - 1]) == null ?
                            method.invoke(originalConfiguration, args) : instance.getConfig(subDataId[len - 1]);
                    }
                }
                return method.invoke(originalConfiguration, args);
            });
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
                parameters.put("type", protocol);
                url = url.substring(index + 3);
            }
            index = url.indexOf(47);
            if (index >= 0) {
                this.path = url.substring(index + 1);
                parameters.put("path", path);
                url = url.substring(0, index);
            }
            this.serverAddr = url;
            parameters.put("serverAddr", serverAddr);
        } else {
            throw new IllegalArgumentException("url == null");
        }
    }

    public String getConfig(String dataId) {
        return parameters.get(dataId);
    }

    public static String getConfigUrlKey() {
        return ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.URL;
    }
}
