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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * @author liuqiufeng
 */
class SimpleFileConfigTest {

    @Test
    void getString() {
        SimpleFileConfig config = new SimpleFileConfig();
        Assertions.assertEquals(File.pathSeparator, config.getString("path.separator"));
        
        config = new SimpleFileConfig(new File("file.conf"), "");
        Assertions.assertEquals("default", config.getString("service.vgroupMapping.default_tx_group"));
        
        config = new SimpleFileConfig(new File("src/test/resources/file"), "file:");
        Assertions.assertEquals("default", config.getString("service.vgroupMapping.default_tx_group"));
    }
}