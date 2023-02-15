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

package io.seata.common.io;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author liuqiufeng
 */
public class FileLoaderTest {

    @Test
    public void testLoadExistFile() {
        File file = FileLoader.load("io/TestFile.txt");
        Assertions.assertTrue(file != null && file.exists());
    }

    @Test
    public void testLoadNotExistFile() {
        File file = FileLoader.load("io/NotExistFile.txt");
        Assertions.assertTrue(file == null || !file.exists());
    }

    @Test
    public void testLoadException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileLoader.load(null));
    }
}
