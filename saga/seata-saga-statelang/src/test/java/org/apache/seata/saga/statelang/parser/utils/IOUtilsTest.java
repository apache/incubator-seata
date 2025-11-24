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
package org.apache.seata.saga.statelang.parser.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class IOUtilsTest {

    @Test
    public void testToStringWithUtf8() throws IOException {
        String content = "test the method of toString";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = IOUtils.toString(inputStream, "UTF-8");

        Assertions.assertEquals(content, result);
    }

    @Test
    public void testCopyInputStreamToWriter() throws IOException {
        String content = "Hello World! This is a test for copy method.";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        StringWriter writer = new StringWriter();

        IOUtils.copy(inputStream, writer);

        Assertions.assertEquals(content, writer.toString());
    }

    @Test
    public void testCopyReaderToWriter() throws IOException {
        String content = "Test copy from Reader to Writer";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes());
        StringWriter writer = new StringWriter();

        int count = IOUtils.copy(new java.io.InputStreamReader(inputStream), writer);

        Assertions.assertEquals(content.length(), count);
        Assertions.assertEquals(content, writer.toString());
    }

    @Test
    public void testToStringWithEmptyStream() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
        String result = IOUtils.toString(inputStream, "UTF-8");
        Assertions.assertEquals("", result);
    }
}
