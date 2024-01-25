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
package org.apache.seata.server.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.MapUtil;
import org.apache.seata.common.util.NumberUtils;
import org.apache.seata.common.util.StringUtils;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

/**
 */
public class PortHelper {

    public static int getPortFromEnvOrStartup(String[] args) {
        int port = 0;
        if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length; ++i) {
                if ("-p".equalsIgnoreCase(args[i]) && i < args.length - 1) {
                    port = NumberUtils.toInt(args[i + 1], 0);
                }
            }
        }
        if (port == 0) {
            port = ContainerHelper.getPort();
        }
        return port;
    }

    /**
     * get config from configFile
     * -Dspring.config.location > classpath:application.properties > classpath:application.yml
     *
     * @return the port
     * @throws IOException the io exception
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
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
            String fileName = configFile.getName();
            String portNum = null;
            if (fileName.endsWith("yml")) {
                Map<String, Object> yamlMap = new Yaml().load(inputStream);
                Map<String, Object> configMap =  MapUtil.getFlattenedMap(yamlMap);
                if (CollectionUtils.isNotEmpty(configMap)) {
                    Object serverPort = configMap.get("server.port");
                    if (null != serverPort) {
                        portNum = serverPort.toString();
                    }
                }
            } else {
                Properties properties = new Properties();
                properties.load(inputStream);
                portNum = properties.getProperty("server.port");
            }
            if (null != portNum) {
                try {
                    port = Integer.parseInt(portNum);
                } catch (NumberFormatException exx) {
                    //ignore
                }
            }
        } finally {
            if (null != inputStream) {
                inputStream.close();
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


}

