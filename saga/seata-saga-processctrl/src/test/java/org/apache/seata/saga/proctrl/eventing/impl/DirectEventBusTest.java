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
package org.apache.seata.saga.proctrl.eventing.impl;

import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.proctrl.eventing.EventConsumer;
import org.apache.seata.saga.proctrl.impl.ProcessContextImpl;
import org.apache.seata.saga.proctrl.mock.MockInstruction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DirectEventBusTest {

    private static final String SYNC_EXECUTION_STACK = "_sync_execution_stack_";

    @Test
    public void testContextToStringAfterReentrantOffer() {
        DirectEventBus bus = new DirectEventBus();
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable("business", "value");
        MockInstruction instruction = new MockInstruction();
        instruction.setTestString("start");
        context.setInstruction(instruction);
        List<ProcessContext> processed = new ArrayList<>();
        registerConsumer(bus, current -> {
            processed.add(current);
            if (processed.size() == 1) {
                Assertions.assertTrue(bus.offer(current));
                Stack<?> stack = (Stack<?>) current.getVariable(SYNC_EXECUTION_STACK);
                Assertions.assertEquals(1, stack.size());
                assertReadable(current, "business=value", "instruction=" + instruction);
                Assertions.assertEquals(1, stack.size());
                Assertions.assertSame(current, stack.peek());
            }
        });

        Assertions.assertTrue(bus.offer(context));
        Assertions.assertEquals(Arrays.asList(context, context), processed);
        Assertions.assertNull(context.getVariable(SYNC_EXECUTION_STACK));
        Assertions.assertEquals("value", context.getVariable("business"));
        Assertions.assertSame(instruction, context.getInstruction());
    }

    @Test
    public void testContextToStringWithPendingSiblingContexts() {
        DirectEventBus bus = new DirectEventBus();
        ProcessContextImpl parent = new ProcessContextImpl();
        parent.setVariable("business", "value");
        ProcessContextImpl first = childContext(parent, "first");
        ProcessContextImpl second = childContext(parent, "second");
        ProcessContextImpl third = childContext(parent, "third");
        List<ProcessContext> processed = new ArrayList<>();
        registerConsumer(bus, current -> {
            processed.add(current);
            if (current == first) {
                Assertions.assertTrue(bus.offer(second));
                Assertions.assertTrue(bus.offer(third));
                Stack<?> stack = (Stack<?>) parent.getVariable(SYNC_EXECUTION_STACK);
                Assertions.assertSame(stack, first.getVariable(SYNC_EXECUTION_STACK));
                Assertions.assertSame(stack, second.getVariable(SYNC_EXECUTION_STACK));
                Assertions.assertSame(stack, third.getVariable(SYNC_EXECUTION_STACK));
                Assertions.assertEquals(2, stack.size());
                assertReadable(first, "state=first", "business=value");
                assertReadable(parent, "business=value");
                assertReadable(second, "state=second", "business=value");
                assertReadable(third, "state=third", "business=value");
                Assertions.assertEquals(2, stack.size());
                Assertions.assertSame(third, stack.peek());
            }
        });

        Assertions.assertTrue(bus.offer(first));
        Assertions.assertEquals(Arrays.asList(first, third, second), processed);
        Assertions.assertNull(parent.getVariable(SYNC_EXECUTION_STACK));
        Assertions.assertEquals("value", parent.getVariable("business"));
        Assertions.assertEquals("second", second.getVariableLocally("state"));
    }

    @Test
    public void testConsumerFailureClearsSharedStackAndAllowsNextOffer() {
        DirectEventBus bus = new DirectEventBus();
        ProcessContextImpl parent = new ProcessContextImpl();
        ProcessContextImpl context = childContext(parent, "current");
        ProcessContextImpl pending = childContext(parent, "pending");
        IllegalStateException failure = new IllegalStateException("consumer failed");
        AtomicInteger calls = new AtomicInteger();
        registerConsumer(bus, current -> {
            Assertions.assertSame(context, current);
            if (calls.incrementAndGet() == 1) {
                Assertions.assertTrue(bus.offer(pending));
                assertReadable(current, "state=current");
                throw failure;
            }
        });

        Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, () -> bus.offer(context)));
        Assertions.assertNull(parent.getVariable(SYNC_EXECUTION_STACK));
        Assertions.assertNull(pending.getVariable(SYNC_EXECUTION_STACK));
        Assertions.assertTrue(bus.offer(context));
        Assertions.assertEquals(2, calls.get());
        Assertions.assertNull(parent.getVariable(SYNC_EXECUTION_STACK));
        Assertions.assertEquals("pending", pending.getVariableLocally("state"));
    }

    private static ProcessContextImpl childContext(ProcessContext parent, String state) {
        ProcessContextImpl context = new ProcessContextImpl();
        context.setParent(parent);
        context.setVariableLocally("state", state);
        return context;
    }

    private static void assertReadable(ProcessContext context, String... values) {
        String output = Assertions.assertDoesNotThrow(context::toString);
        for (String value : values) {
            Assertions.assertTrue(output.contains(value), output);
        }
    }

    private static void registerConsumer(DirectEventBus bus, Consumer<ProcessContext> consumer) {
        bus.registerEventConsumer(new EventConsumer<ProcessContext>() {
            @Override
            public void process(ProcessContext event) {
                consumer.accept(event);
            }

            @Override
            public boolean accept(Class<ProcessContext> clazz) {
                return ProcessContext.class.isAssignableFrom(clazz);
            }
        });
    }
}
