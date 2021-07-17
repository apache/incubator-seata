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
package io.seata.config.file;

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.config.FileConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author wangwei-ying
 */
@LoadLevel(name = FileConfigFactory.YAML_TYPE, order = 1, scope = Scope.PROTOTYPE)
public class YamlFileConfig implements FileConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlFileConfig.class);
    private Map configMap;

    public YamlFileConfig(File file, String name) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream is = new FileInputStream(file)) {
            configMap = yaml.load(is);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("file not found");
        }
    }

    @Override
    public String getString(String path) {
        try {
            Map config = configMap;
            String[] dataId = path.split("\\.");
            for (int i = 0; i < dataId.length - 1; i++) {
                if (config.containsKey(dataId[i])) {
                    config = (Map) config.get(dataId[i]);
                } else {
                    return null;
                }
            }
            Object value = config.get(dataId[dataId.length - 1]);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            LOGGER.warn("get config data error" + path, e);
            return null;
        }
    }
}
