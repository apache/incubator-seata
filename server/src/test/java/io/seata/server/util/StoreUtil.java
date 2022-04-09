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
package io.seata.server.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;

import static io.seata.server.session.SessionHolder.DEFAULT_SESSION_STORE_FILE_DIR;

public class StoreUtil {

    private static String sessionStorePath =
        ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.STORE_FILE_DIR, DEFAULT_SESSION_STORE_FILE_DIR);

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
