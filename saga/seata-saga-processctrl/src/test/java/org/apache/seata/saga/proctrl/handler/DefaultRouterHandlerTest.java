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
package org.apache.seata.saga.proctrl.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.saga.proctrl.ProcessRouter;
import org.apache.seata.saga.proctrl.ProcessType;
import org.apache.seata.saga.proctrl.impl.ProcessContextImpl;
import org.apache.seata.saga.proctrl.mock.MockProcessRouter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * DefaultRouterHandlerTest
 */
public class DefaultRouterHandlerTest {
    @Test
    public void testRouteOfFrameworkException() {
        ProcessContextImpl context = new ProcessContextImpl();
        DefaultRouterHandler defaultRouterHandler = new DefaultRouterHandler();
        Assertions.assertThrows(FrameworkException.class, () -> defaultRouterHandler.route(context));
    }

    @Test
    public void testRouteOfException() {
        ProcessContextImpl context = new ProcessContextImpl();
        context.setVariable("exception", new Object());
        DefaultRouterHandler defaultRouterHandler = new DefaultRouterHandler();
        Map<String, ProcessRouter> processRouters = new HashMap<>();
        ProcessRouter processRouter = new MockProcessRouter();
        processRouters.put(ProcessType.STATE_LANG.getCode(), processRouter);
        defaultRouterHandler.setProcessRouters(processRouters);
        Assertions.assertThrows(RuntimeException.class, () -> defaultRouterHandler.route(context));
    }
}