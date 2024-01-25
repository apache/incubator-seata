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
package org.apache.seata.server.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.seata.common.XID;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;

import static org.apache.seata.common.DefaultValues.DEFAULT_SESSION_STORE_FILE_DIR;
import static java.io.File.separator;

/**
 */
public class StoreUtil {

    private static String sessionStorePath =
        ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR)
            + separator + XID.getPort();

    public static void deleteDataFile() {
        try {
            File directory = new File(sessionStorePath);
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.exists()) {
                        Files.delete(Paths.get(file.getPath()));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
