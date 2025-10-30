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
package org.apache.seata.common.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @Test
    public void testLoadWhenDirectPath() throws Exception {
        Path tempFile = Paths.get("direct-test-file.txt");
        Files.createFile(tempFile);

        File result = FileLoader.load("direct-test-file.txt");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.exists());

        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testLoadWhenSpecial() throws Exception {
        String encodedName = "测试%20文件.txt";
        String decodedName = "测试 文件.txt";

        Path tempFile = Paths.get(decodedName);
        Files.createFile(tempFile);

        File result = FileLoader.load(encodedName);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.exists());

        Files.deleteIfExists(tempFile);
    }

    @Test
    public void testLoadWhenNull() {
        File result = FileLoader.load("nonexistent/path.txt");
        Assertions.assertNull(result);
    }
}
