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
package org.apache.seata.saga.statelang.domain.impl;

import org.apache.seata.saga.statelang.domain.StateType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ScriptTaskStateImplTest {

    @Test
    public void testScriptTaskStateImplDefault() {
        ScriptTaskStateImpl scriptTask = new ScriptTaskStateImpl();

        assertEquals(StateType.SCRIPT_TASK, scriptTask.getType(), "ScriptTaskStateImpl should be SCRIPT_TASK");
        assertEquals("groovy", scriptTask.getScriptType(), "default should be groovy");
        assertNull(scriptTask.getScriptContent());
    }

    @Test
    public void testScriptTaskStateImplCustom() {
        ScriptTaskStateImpl scriptTask = new ScriptTaskStateImpl();

        scriptTask.setScriptType("python");
        scriptTask.setScriptContent("print('hello')");

        assertEquals("python", scriptTask.getScriptType());
        assertEquals("print('hello')", scriptTask.getScriptContent());
    }
}
