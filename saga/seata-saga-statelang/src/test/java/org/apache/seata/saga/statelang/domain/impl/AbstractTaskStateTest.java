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

import org.apache.seata.saga.statelang.domain.TaskState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractTaskStateTest {

    private static class TestAbstractTaskState extends AbstractTaskState {}

    @Test
    public void testAbstractTaskStateGettersAndSetters() {
        AbstractTaskState taskState = new TestAbstractTaskState();
        List<TaskState.Retry> retries = new ArrayList<>();
        List<TaskState.ExceptionMatch> catches = new ArrayList<>();
        List<Object> input = new ArrayList<>();
        Map<String, Object> output = new HashMap<>();
        Map<String, String> status = new HashMap<>();
        List<Object> inputExpressions = new ArrayList<>();
        Map<String, Object> outputExpressions = new HashMap<>();
        TaskState.Loop loop = new AbstractTaskState.LoopImpl();

        taskState.setCompensateState("compensateState1");
        taskState.setForCompensation(true);
        taskState.setRetry(retries);
        taskState.setCatches(catches);
        taskState.setInput(input);
        taskState.setOutput(output);
        taskState.setStatus(status);
        taskState.setPersist(false);
        taskState.setRetryPersistModeUpdate(true);
        taskState.setCompensatePersistModeUpdate(false);
        taskState.setInputExpressions(inputExpressions);
        taskState.setOutputExpressions(outputExpressions);
        taskState.setLoop(loop);

        assertEquals("compensateState1", taskState.getCompensateState());
        assertTrue(taskState.isForCompensation());
        Assertions.assertSame(retries, taskState.getRetry());
        Assertions.assertSame(catches, taskState.getCatches());
        Assertions.assertSame(input, taskState.getInput());
        Assertions.assertSame(output, taskState.getOutput());
        Assertions.assertSame(status, taskState.getStatus());
        Assertions.assertFalse(taskState.isPersist());
        assertTrue(taskState.isRetryPersistModeUpdate());
        Assertions.assertFalse(taskState.isCompensatePersistModeUpdate());
        Assertions.assertSame(inputExpressions, taskState.getInputExpressions());
        Assertions.assertSame(outputExpressions, taskState.getOutputExpressions());
        Assertions.assertSame(loop, taskState.getLoop());
    }

    @Test
    public void testSetCompensateState_WithNonBlankValue() {
        AbstractTaskState taskState = new TestAbstractTaskState();
        taskState.setForUpdate(false);

        taskState.setCompensateState("compensateState2");

        assertEquals("compensateState2", taskState.getCompensateState());
        assertTrue(taskState.isForUpdate());
    }

    @Test
    public void testSetCompensateState_WithBlankValue() {
        AbstractTaskState taskState = new TestAbstractTaskState();
        taskState.setForUpdate(true);

        taskState.setCompensateState("");

        assertEquals("", taskState.getCompensateState());
        assertTrue(taskState.isForUpdate());
    }

    @Test
    public void testRetryImpl() {
        AbstractTaskState.RetryImpl retry = new AbstractTaskState.RetryImpl();
        List<String> exceptions = new ArrayList<>();
        exceptions.add("Exception1");
        List<Class<? extends Exception>> exceptionClasses = new ArrayList<>();
        exceptionClasses.add(RuntimeException.class);

        retry.setExceptions(exceptions);
        retry.setExceptionClasses(exceptionClasses);
        retry.setIntervalSeconds(1.5);
        retry.setMaxAttempts(3);
        retry.setBackoffRate(2.0);

        Assertions.assertSame(exceptions, retry.getExceptions());
        Assertions.assertSame(exceptionClasses, retry.getExceptionClasses());
        assertEquals(1.5, retry.getIntervalSeconds());
        assertEquals(3, retry.getMaxAttempts());
        assertEquals(2.0, retry.getBackoffRate());
    }

    @Test
    public void testExceptionMatchImpl() {
        AbstractTaskState.ExceptionMatchImpl exceptionMatch = new AbstractTaskState.ExceptionMatchImpl();
        List<String> exceptions = new ArrayList<>();
        exceptions.add("Exception2");
        List<Class<? extends Exception>> exceptionClasses = new ArrayList<>();
        exceptionClasses.add(IllegalArgumentException.class);

        exceptionMatch.setExceptions(exceptions);
        exceptionMatch.setExceptionClasses(exceptionClasses);
        exceptionMatch.setNext("nextState");

        Assertions.assertSame(exceptions, exceptionMatch.getExceptions());
        Assertions.assertSame(exceptionClasses, exceptionMatch.getExceptionClasses());
        assertEquals("nextState", exceptionMatch.getNext());
    }

    @Test
    public void testLoopImpl() {
        AbstractTaskState.LoopImpl loop = new AbstractTaskState.LoopImpl();

        loop.setParallel(5);
        loop.setCollection("collectionVar");
        loop.setElementVariableName("element");
        loop.setElementIndexName("index");
        loop.setCompletionCondition("${count > 10}");

        assertEquals(5, loop.getParallel());
        assertEquals("collectionVar", loop.getCollection());
        assertEquals("element", loop.getElementVariableName());
        assertEquals("index", loop.getElementIndexName());
        assertEquals("${count > 10}", loop.getCompletionCondition());
    }
}
