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
package org.apache.seata.saga.engine.pcext.utils;

import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link LoopContextHolder}
 */
public class LoopContextHolderTest {

    @Test
    public void getCurrentWhenNotExistAndForceCreateFalseReturnNullTest() {
        ProcessContext context = mock(ProcessContext.class);
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(null);

        LoopContextHolder result = LoopContextHolder.getCurrent(context, false);

        assertNull(result);
    }

    @Test
    public void getCurrentWhenNotExistAndForceCreateTrueCreateNewTest() {
        ProcessContext context = mock(ProcessContext.class);
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(null);

        LoopContextHolder result = LoopContextHolder.getCurrent(context, true);

        assertNotNull(result);
        verify(context)
                .setVariable(eq(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER), any(LoopContextHolder.class));
    }

    @Test
    public void getCurrentWhenExistReturnExistingTest() {
        ProcessContext context = mock(ProcessContext.class);
        LoopContextHolder existing = new LoopContextHolder();
        when(context.getVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER))
                .thenReturn(existing);

        LoopContextHolder result = LoopContextHolder.getCurrent(context, true);

        assertSame(existing, result);
    }

    @Test
    public void clearCurrentRemoveVariableTest() {
        ProcessContext context = mock(ProcessContext.class);

        LoopContextHolder.clearCurrent(context);

        verify(context).removeVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER);
    }

    @Test
    public void atomicCountersThreadSafeTest() {
        LoopContextHolder holder = new LoopContextHolder();

        holder.getNrOfInstances().set(10);
        holder.getNrOfActiveInstances().incrementAndGet();
        holder.getNrOfCompletedInstances().addAndGet(5);

        assertEquals(10, holder.getNrOfInstances().get());
        assertEquals(1, holder.getNrOfActiveInstances().get());
        assertEquals(5, holder.getNrOfCompletedInstances().get());
    }

    @Test
    public void stackOperationsTest() {
        LoopContextHolder holder = new LoopContextHolder();

        holder.getLoopCounterStack().push(1);
        holder.getLoopCounterStack().push(2);
        holder.getForwardCounterStack().push(10);

        assertEquals(Integer.valueOf(2), holder.getLoopCounterStack().pop());
        assertEquals(Integer.valueOf(1), holder.getLoopCounterStack().pop());
        assertEquals(Integer.valueOf(10), holder.getForwardCounterStack().pop());
    }

    @Test
    public void collectionGetterSetterTest() {
        LoopContextHolder holder = new LoopContextHolder();
        List<String> collection = Arrays.asList("a", "b", "c");

        holder.setCollection(collection);

        assertSame(collection, holder.getCollection());
    }

    @Test
    public void failEndFlagTest() {
        LoopContextHolder holder = new LoopContextHolder();

        assertFalse(holder.isFailEnd());
        holder.setFailEnd(true);
        assertTrue(holder.isFailEnd());
    }

    @Test
    public void completionConditionSatisfiedFlagTest() {
        LoopContextHolder holder = new LoopContextHolder();

        assertFalse(holder.isCompletionConditionSatisfied());
        holder.setCompletionConditionSatisfied(true);
        assertTrue(holder.isCompletionConditionSatisfied());
    }
}
