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
package io.seata.server.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NumberUtils;
import io.seata.common.util.StringUtils;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * @author wang.liang
 */
public class PortHelper {

    public static int getPortFromEnvOrStartup(String[] args) {
        if (ContainerHelper.isRunningInContainer()) {
            return ContainerHelper.getPort();
        } else if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length; ++i) {
                if ("-p".equalsIgnoreCase(args[i]) && i < args.length - 1) {
                    return NumberUtils.toInt(args[i + 1], 0);
                }
            }
        }

        return 0;
    }

    /**
     * get config from configFile
     * -Dspring.config.location > classpath:application.properties > classpath:application.yml
     *
     * @return
     * @throws IOException
     */
    public static int getPortFromConfigFile() throws IOException {

        int port = 8080;
        File configFile = null;
        File startupConfigFile = getConfigFromStartup();
        if (null != startupConfigFile) {
            configFile = startupConfigFile;
        } else {
            try {
                File propertiesFile = ResourceUtils.getFile("classpath:application.properties");
                configFile = propertiesFile;
            } catch (FileNotFoundException exx) {
                File ymlFile = ResourceUtils.getFile("classpath:application.yml");
                configFile = ymlFile;
            }
        }
        String fileName = configFile.getName();
        String portNum = null;
        if (fileName.endsWith("yml")) {
            Map<String, Object> configMap = new HashMap<>();
            Map<String, Object> yamlMap = new Yaml().load(new FileInputStream(configFile));
            bulidFlatMap(yamlMap, null, configMap);
            if (CollectionUtils.isNotEmpty(configMap)) {
                Object serverPort = configMap.get("server.port");
                if (null != serverPort) {
                    portNum = serverPort.toString();
                }
            }
        } else {
            Properties properties = new Properties();
            properties.load(new FileInputStream(configFile));
            portNum = properties.getProperty("server.port");
        }
        if (null != portNum) {
            try {
                port = Integer.parseInt(portNum);
            } catch (NumberFormatException exx) {
                //ignore
            }
        }
        return port;

    }

    private static File getConfigFromStartup() {

        String configLocation = System.getProperty("spring.config.location");
        if (StringUtils.isNotBlank(configLocation)) {
            try {
                File configFile = ResourceUtils.getFile(configLocation);
                if (!configFile.isFile()) {
                    return null;
                }
                String fileName = configFile.getName();
                if (!(fileName.endsWith("yml") || fileName.endsWith("properties"))) {
                    return null;
                }
                return configFile;
            } catch (FileNotFoundException e) {
                return null;
            }
        }
        return null;

    }

    public static void bulidFlatMap(Map<String, Object> sourceMap, String prefix, Map<String, Object> resultMap) {
        if (CollectionUtils.isNotEmpty(sourceMap)) {
            for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
                Object value = entry.getValue();
                String key = entry.getKey();
                if (StringUtils.isNotBlank(prefix)) {
                    key = prefix + "." + key;
                }
                if (value instanceof String) {
                    resultMap.put(key, value);
                } else if (value instanceof Map) {
                    bulidFlatMap((Map<String, Object>)value, key, resultMap);
                } else if (value instanceof Collection) {
                    if (((Collection)value).isEmpty()) {
                        sourceMap.put(key, "");
                    } else {
                        int index = 0;
                        Collection collection = (Collection)value;
                        for (Object obj : collection) {
                            bulidFlatMap(Collections.singletonMap("[" + (index++) + "]", obj), key, resultMap);
                        }
                    }
                } else {
                    resultMap.put(key, value == null ? "null" : value);
                }
            }
        }
    }
}

  