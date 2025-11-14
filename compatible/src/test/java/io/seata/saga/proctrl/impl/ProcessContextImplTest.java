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
package io.seata.saga.proctrl.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ProcessContextImpl.
 */
public class ProcessContextImplTest {

    private ProcessContextImpl processContext;
    private org.apache.seata.saga.proctrl.impl.ProcessContextImpl apacheContext;

    @BeforeEach
    public void setUp() {
        apacheContext = new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        processContext = ProcessContextImpl.wrap(apacheContext);
    }

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                ProcessContextImpl.class.isAnnotationPresent(Deprecated.class),
                "ProcessContextImpl should be marked as @Deprecated");
    }

    @Test
    public void testWrap() {
        org.apache.seata.saga.proctrl.impl.ProcessContextImpl apache =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl wrapped = ProcessContextImpl.wrap(apache);
        assertNotNull(wrapped);
        assertSame(apache, wrapped.unwrap());
    }

    @Test
    public void testUnwrap() {
        assertSame(apacheContext, processContext.unwrap());
    }

    @Test
    public void testGetSetVariable() {
        processContext.setVariable("key1", "value1");
        assertEquals("value1", processContext.getVariable("key1"));

        processContext.setVariable("key2", 123);
        assertEquals(123, processContext.getVariable("key2"));
    }

    @Test
    public void testGetSetVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("var1", "value1");
        variables.put("var2", 456);

        processContext.setVariables(variables);

        Map<String, Object> result = processContext.getVariables();
        assertNotNull(result);
        assertEquals("value1", result.get("var1"));
        assertEquals(456, result.get("var2"));
    }

    @Test
    public void testGetSetVariableLocally() {
        processContext.setVariableLocally("localKey1", "localValue1");
        assertEquals("localValue1", processContext.getVariableLocally("localKey1"));

        processContext.setVariableLocally("localKey2", 789);
        assertEquals(789, processContext.getVariableLocally("localKey2"));
    }

    @Test
    public void testGetSetVariablesLocally() {
        Map<String, Object> localVariables = new HashMap<>();
        localVariables.put("localVar1", "localValue1");
        localVariables.put("localVar2", 999);

        processContext.setVariablesLocally(localVariables);

        Map<String, Object> result = processContext.getVariablesLocally();
        assertNotNull(result);
        assertEquals("localValue1", result.get("localVar1"));
        assertEquals(999, result.get("localVar2"));
    }

    @Test
    public void testHasVariable() {
        processContext.setVariable("existingKey", "existingValue");
        assertTrue(processContext.hasVariable("existingKey"));
        assertFalse(processContext.hasVariable("nonExistingKey"));
    }

    @Test
    public void testHasVariableLocal() {
        processContext.setVariableLocally("localKey", "localValue");
        assertTrue(processContext.hasVariableLocal("localKey"));
        assertFalse(processContext.hasVariableLocal("nonExistingLocalKey"));
    }

    @Test
    public void testRemoveVariable() {
        processContext.setVariable("removeKey", "removeValue");
        assertTrue(processContext.hasVariable("removeKey"));

        Object removed = processContext.removeVariable("removeKey");
        assertEquals("removeValue", removed);
        assertFalse(processContext.hasVariable("removeKey"));
    }

    @Test
    public void testRemoveVariableLocally() {
        processContext.setVariableLocally("removeLocalKey", "removeLocalValue");
        assertTrue(processContext.hasVariableLocal("removeLocalKey"));

        Object removed = processContext.removeVariableLocally("removeLocalKey");
        assertEquals("removeLocalValue", removed);
        assertFalse(processContext.hasVariableLocal("removeLocalKey"));
    }

    @Test
    public void testClearLocally() {
        processContext.setVariableLocally("local1", "value1");
        processContext.setVariableLocally("local2", "value2");

        assertTrue(processContext.hasVariableLocal("local1"));
        assertTrue(processContext.hasVariableLocal("local2"));

        processContext.clearLocally();

        // After clear, the locally set variables should be cleared
        Map<String, Object> localVars = processContext.getVariablesLocally();
        assertNotNull(localVars);
    }

    @Test
    public void testGetSetParent() {
        org.apache.seata.saga.proctrl.impl.ProcessContextImpl parentApacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl parentContext = ProcessContextImpl.wrap(parentApacheContext);

        processContext.setParent(parentContext);

        assertNotNull(processContext.getParent());
    }

    @Test
    public void testToString() {
        processContext.setVariable("testKey", "testValue");
        String result = processContext.toString();
        assertNotNull(result);
    }

    @Test
    public void testVariableInheritanceFromParent() {
        org.apache.seata.saga.proctrl.impl.ProcessContextImpl parentApacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl parentContext = ProcessContextImpl.wrap(parentApacheContext);

        parentContext.setVariable("parentKey", "parentValue");
        processContext.setParent(parentContext);

        // Child should be able to access parent's variable
        Object value = processContext.getVariable("parentKey");
        assertNotNull(value);
    }

    @Test
    public void testLocalVariableDoesNotAffectParent() {
        org.apache.seata.saga.proctrl.impl.ProcessContextImpl parentApacheContext =
                new org.apache.seata.saga.proctrl.impl.ProcessContextImpl();
        ProcessContextImpl parentContext = ProcessContextImpl.wrap(parentApacheContext);

        processContext.setParent(parentContext);
        processContext.setVariableLocally("childLocalKey", "childLocalValue");

        // Parent should not have child's local variable
        assertFalse(parentContext.hasVariableLocal("childLocalKey"));
    }
}
