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

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.file.FileConfig;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author wangwei-ying
 */
public class FileConfigFactory {

    public static final String DEFAULT_TYPE = "CONF";

    public static final String YAML_TYPE = "YAML";

    private static final LinkedHashMap<String, String> SUFFIX_MAP = new LinkedHashMap<String, String>(4) {
        {
            put("conf", DEFAULT_TYPE);
            put("properties", DEFAULT_TYPE);
            put("yml", YAML_TYPE);
        }
    };


    public static FileConfig load() {
        return loadService(DEFAULT_TYPE, null, null);
    }

    public static FileConfig load(File targetFile, String name) {
        String fileName = targetFile.getName();
        String configType = getConfigType(fileName);
        return loadService(configType, new Class[]{File.class, String.class}, new Object[]{targetFile, name});
    }

    private static String getConfigType(String fileName) {
        String configType = DEFAULT_TYPE;
        int suffixIndex = fileName.lastIndexOf(".");
        if (suffixIndex > 0) {
            configType = SUFFIX_MAP.getOrDefault(fileName.substring(suffixIndex + 1), DEFAULT_TYPE);
        }

        return configType;
    }

    private static FileConfig loadService(String name, Class[] argsType, Object[] args) {
        FileConfig fileConfig = EnhancedServiceLoader.load(FileConfig.class, name, argsType, args);
        return fileConfig;
    }

    public static Set<String> getSuffixSet() {
        return SUFFIX_MAP.keySet();
    }

    public synchronized static void register(String suffix, String beanActiveName) {
        SUFFIX_MAP.put(suffix, beanActiveName);
    }


}
