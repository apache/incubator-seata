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

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.json.JsonSerializerFactory;
import org.apache.seata.saga.statelang.parser.JsonParser;
import org.apache.seata.saga.statelang.parser.JsonParserFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class DesignerJsonTransformerTest {

    @Test
    public void testToStandardJsonWithDesignerJson() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        JsonParser jsonParser = JsonParserFactory.getJsonParser("jackson");
        Map<String, Object> designerJson = jsonParser.parse(json, Map.class, true);

        Map<String, Object> standardJson = DesignerJsonTransformer.toStandardJson(designerJson);

        Assertions.assertNotNull(standardJson);
        Assertions.assertTrue(standardJson.containsKey("States"));
        Assertions.assertTrue(standardJson.containsKey("StartState"));
        Assertions.assertEquals("simpleTestStateMachine", standardJson.get("Name"));
    }

    @Test
    public void testToStandardJsonWithNonDesignerJson() {
        Map<String, Object> normalJson = new HashMap<>();
        normalJson.put("Name", "normal");
        normalJson.put("States", new HashMap<>());

        Map<String, Object> result = DesignerJsonTransformer.toStandardJson(normalJson);

        Assertions.assertSame(normalJson, result);
    }

    @Test
    public void testGenerateTracingGraphJsonWithNullInstance() {
        JsonSerializer parser = JsonSerializerFactory.getSerializer("jackson");
        FrameworkException e = Assertions.assertThrows(
                FrameworkException.class, () -> DesignerJsonTransformer.generateTracingGraphJson(null, parser));
        Assertions.assertEquals("StateMachineInstance is not exits", e.getMessage());
    }

    private InputStream getInputStreamByPath(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getClass().getClassLoader();
        }
        return classLoader.getResourceAsStream(path);
    }
}
