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
package org.apache.seata.saga.statelang.validator.impl;

import org.apache.seata.saga.statelang.parser.StateMachineParserFactory;
import org.apache.seata.saga.statelang.parser.utils.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Unit tests for {@link StateNameExistsRule}
 */
public class StateNameExistsRuleTest {

    @Test
    public void testAllStateNamesExist() throws IOException {
        // Test state machine with all subsequent states existing
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine.json");
        String json = IOUtils.toString(inputStream, "UTF-8");

        Assertions.assertDoesNotThrow(() -> {
            StateMachineParserFactory.getStateMachineParser(null).parse(json);
        });
    }

    private InputStream getInputStreamByPath(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getClass().getClassLoader();
        }
        return classLoader.getResourceAsStream(path);
    }
}
