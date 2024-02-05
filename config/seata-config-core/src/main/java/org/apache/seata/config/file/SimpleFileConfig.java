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
package org.apache.seata.config.file;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.config.FileConfigFactory;
import org.apache.seata.config.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


@LoadLevel(name = FileConfigFactory.DEFAULT_TYPE,scope = Scope.PROTOTYPE)
public class SimpleFileConfig implements FileConfig {

    private Config fileConfig;

    public SimpleFileConfig() {
        fileConfig = ConfigFactory.load();
    }

    public SimpleFileConfig(File file, String name) {
        if (name.startsWith(FileConfiguration.SYS_FILE_RESOURCE_PREFIX)) {
            Config appConfig = ConfigFactory.parseFileAnySyntax(file);
            fileConfig = ConfigFactory.load(appConfig);
        } else {
            fileConfig = ConfigFactory.load(file.getName());
        }
    }

    @Override
    public String getString(String path) {
        return fileConfig.getString(path);
    }

    @Override
    public Map<String, Object> getAllConfig() {
        return fileConfig.entrySet().stream().collect(HashMap::new, (m, e) ->
                        m.put(e.getKey(), e.getValue().unwrapped()),
            HashMap::putAll);
    }

}
