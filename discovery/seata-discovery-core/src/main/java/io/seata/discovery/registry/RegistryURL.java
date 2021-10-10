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
import io.seata.config.Configuration;
import io.seata.config.ConfigurationKeys;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * use url to config tne register center
 *
 * @author liujian
 */
public class RegistryURL {

    /**
     * serverAddr
     */
    private String serverAddr;
    private String protocol;
    private String path;
    /**
     * register center paramters
     */
    private Map<String, String> parameters = new HashMap<>();
    private static final String METHOD_PREFIX = "get";
    private static final String METHOD_LATEST_CONFIG = METHOD_PREFIX + "LatestConfig";

    private static RegistryURL instance;

    public static RegistryURL getInstance(Configuration configuration) {
        if (instance == null) {
            synchronized (RegistryURL.class) {
                if (StringUtils.isNotBlank(configuration.getConfig(getRegistryUrlKey()))) {
                    String url = configuration.getConfig(getRegistryUrlKey());
                    instance = new RegistryURL(url);
                }
            }
        }
        return instance;
    }

    public RegistryURL(String url) {
        valueOf(url);
    }

    public static Configuration proxy(Configuration originalConfiguration) {
        String url = originalConfiguration.getConfig(getRegistryUrlKey());
        return (Configuration) Enhancer.create(Configuration.class,
                (MethodInterceptor) (proxy, method, args, methodProxy) -> {
                    if (method.getName().startsWith(METHOD_PREFIX)
                            && !method.getName().equalsIgnoreCase(METHOD_LATEST_CONFIG)) {
                        String rawDataId = (String) args[0];
                        if (StringUtils.isNotBlank(url)) {
                            getInstance(originalConfiguration);
                            String[] subDataId = rawDataId.split("\\.");
                            return instance.getConfig(subDataId[subDataId.length - 1]);
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

    public static String getRegistryUrlKey() {
        return ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.URL;
    }

}