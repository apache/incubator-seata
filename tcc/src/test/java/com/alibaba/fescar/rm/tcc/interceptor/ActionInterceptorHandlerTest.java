/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.alibaba.fescar.rm.tcc.interceptor;

import com.alibaba.fescar.rm.tcc.TccAction;
import com.alibaba.fescar.rm.tcc.TccParam;
import com.alibaba.fescar.rm.tcc.api.BusinessActionContext;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Action interceptor handler test.
 *
 * @author zhangsen
 */
public class ActionInterceptorHandlerTest {

    /**
     * The Action interceptor handler.
     */
    protected  ActionInterceptorHandler actionInterceptorHandler = new ActionInterceptorHandler();

    /**
     * Test business action context.
     *
     * @throws NoSuchMethodException the no such method exception
     */
    @Test
    public void testBusinessActionContext() throws NoSuchMethodException {
        Method prepareMethod = TccAction.class.getDeclaredMethod("prepare",
                new Class[]{BusinessActionContext.class, int.class, List.class, TccParam.class});
        List list = new ArrayList();
        list.add("b");
        TccParam tccParam = new TccParam (1, "abc@ali.com");

        Map<String, Object>  paramContext = actionInterceptorHandler.fetchActionRequestContext(prepareMethod,
                new Object[]{null, 10, list, tccParam});
        System.out.println(paramContext);

        Assert.assertEquals(10, paramContext.get("a"));
        Assert.assertEquals("b", paramContext.get("b"));
        Assert.assertEquals("abc@ali.com", paramContext.get("email"));
    }

}