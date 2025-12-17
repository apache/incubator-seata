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
package org.seata.mcp.core.runtimeCore;

import org.apache.seata.mcp.core.runtimeCore.DefaultRuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContext;
import org.apache.seata.mcp.core.runtimeCore.RuntimeContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.function.ServerRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class RuntimeContextTest {

    @Test
    void testDefaultRuntimeContextStoresValues() {
        DefaultRuntimeContext context = new DefaultRuntimeContext();
        assertNull(context.get("missing"));

        context.put("key", "value");
        assertEquals("value", context.get("key"));
    }

    @Test
    void testCopyCreatesDefensiveCopy() {
        DefaultRuntimeContext context = new DefaultRuntimeContext();
        context.put("key", "value");

        RuntimeContext copy = context.copy();
        assertEquals("value", copy.get("key"));

        copy.put("key2", "other");
        assertNull(context.get("key2"));
    }

    @Test
    void testRuntimeContextEmptyConstant() {
        assertNull(RuntimeContext.EMPTY.get("any"));
        RuntimeContext copy = RuntimeContext.EMPTY.copy();
        org.junit.jupiter.api.Assertions.assertEquals(DefaultRuntimeContext.class, copy.getClass());
    }

    @Test
    void testResolverReturnsProvidedContext() {
        RuntimeContextResolver resolver = new RuntimeContextResolver();
        RuntimeContext context = new DefaultRuntimeContext();
        context.put("k", "v");

        RuntimeContext extracted = resolver.extract(mock(ServerRequest.class), context);
        assertSame(context, extracted);
    }

    @Test
    void testResolverReturnsEmptyWhenNull() {
        RuntimeContextResolver resolver = new RuntimeContextResolver();

        RuntimeContext extracted = resolver.extract(mock(ServerRequest.class), null);
        assertSame(RuntimeContext.EMPTY, extracted);
    }
}
