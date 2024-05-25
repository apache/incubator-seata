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
package org.apache.seata.saga.proctrl.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ProcessContextImplTest
 */
public class ProcessContextImplTest {

    @Test
    public void testGetVariableFromParent() {
        ProcessContextImpl context = new ProcessContextImpl();
        ProcessContextImpl parentContext = new ProcessContextImpl();
        parentContext.setVariable("key", "value");
        context.setParent(parentContext);
        Assertions.assertEquals("value", context.getVariable("key"));
    }

    @Test
    public void testSetVariable() {
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable("key", "value");
        context.setVariable("key", "value1");
        Assertions.assertEquals("value1", context.getVariable("key"));
        context.removeVariable("key");
        ProcessContextImpl parentContext = new ProcessContextImpl();
        parentContext.setVariable("key", "value");
        context.setParent(parentContext);
        Assertions.assertEquals("value", context.getVariable("key"));
    }

    @Test
    public void testGetVariables() {
        ProcessContextImpl context = new ProcessContextImpl();
        ProcessContextImpl parentContext = new ProcessContextImpl();
        parentContext.setVariable("key", "value");
        context.setParent(parentContext);
        Assertions.assertEquals(1, context.getVariables().size());
    }

    @Test
    public void testSetVariables() {
        ProcessContextImpl context = new ProcessContextImpl();
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        context.setVariables(map);
        Assertions.assertEquals(1, context.getVariables().size());
    }

    @Test
    public void testGetVariableLocally() {
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable("key", "value");
        Assertions.assertEquals("value", context.getVariableLocally("key"));
    }


    @Test
    public void testSetVariablesLocally() {
        ProcessContextImpl context = new ProcessContextImpl();
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        context.setVariablesLocally(map);
        Assertions.assertEquals("value", context.getVariableLocally("key"));
    }

    @Test
    public void testHasVariable() {
        ProcessContextImpl context = new ProcessContextImpl();
        Assertions.assertFalse(context.hasVariable("key"));
    }

    @Test
    public void testRemoveVariable() {
        ProcessContextImpl context = new ProcessContextImpl();
        ProcessContextImpl parentContext = new ProcessContextImpl();
        parentContext.setVariable("key", "value");
        context.setParent(parentContext);
        context.setVariable("key1", "value1");
        context.removeVariable("key");
        context.removeVariable("key1");
        Assertions.assertEquals(0, context.getVariables().size());
    }

    @Test
    public void testRemoveVariableLocally() {
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable("key", "value");
        context.removeVariableLocally("key");
        Assertions.assertEquals(0, context.getVariables().size());
    }

    @Test
    public void testClearLocally() {
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable("key", "value");
        context.clearLocally();
        Assertions.assertEquals(0, context.getVariables().size());
    }
}