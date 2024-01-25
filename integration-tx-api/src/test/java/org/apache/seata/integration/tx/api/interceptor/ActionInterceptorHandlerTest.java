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
package org.apache.seata.integration.tx.api.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Action interceptor handler test.
 *
 */
public class ActionInterceptorHandlerTest {

    /**
     * The Action interceptor handler.
     */
    protected ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    /**
     * Test business action context.
     *
     * @throws NoSuchMethodException the no such method exception
     */
    @Test
    public void testBusinessActionContext() throws NoSuchMethodException {
        Method prepareMethod = TestAction.class.getDeclaredMethod("prepare",
                BusinessActionContext.class, int.class, List.class, TestParam.class);
        List<Object> list = new ArrayList<>();
        list.add("b");
        TestParam tccParam = new TestParam(1, "abc@ali.com");

        Map<String, Object>  paramContext = actionInterceptorHandler.fetchActionRequestContext(prepareMethod,
                new Object[]{null, 10, list, tccParam});
        System.out.println(paramContext);

        Assertions.assertEquals(10, paramContext.get("a"));
        Assertions.assertEquals("b", paramContext.get("b"));
        Assertions.assertEquals("abc@ali.com", paramContext.get("email"));
    }

}
