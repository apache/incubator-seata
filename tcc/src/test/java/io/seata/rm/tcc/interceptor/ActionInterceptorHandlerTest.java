/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.rm.tcc.interceptor;

import io.seata.rm.tcc.TccAction;
import io.seata.rm.tcc.TccParam;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Action interceptor handler test.
 *
 * @author zhangsen
 */
class ActionInterceptorHandlerTest {

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
    void testBusinessActionContext() throws NoSuchMethodException {
        Method prepareMethod = TccAction.class.getDeclaredMethod("prepare",
                BusinessActionContext.class, int.class, List.class, long[].class,
                TccParam.class, TccParam.class, TccParam.class, boolean.class, boolean.class);

        // int a
        int intA = 11;

        // list b
        List<Object> listB = new ArrayList<>();
        listB.add("bb");

        // array c
        long[] arrayC = new long[2];
        arrayC[0] = 3L;
        arrayC[1] = 33L;

        // object d
        TccParam objD = new TccParam(1, "aaa", "aaa@ali.com", null, true);

        // object e
        TccParam objE = new TccParam(2, "bbb", null, "bbb is an IT man", false);

        // object f
        TccParam objF = new TccParam(3, "ccc", "ccc@ali.com", "ccc is a strong man", null);

        // boolean g
        boolean g = true;

        // boolean h
        boolean h = false;


        // fetch context
        Map<String, Object> paramContext = actionInterceptorHandler.fetchActionRequestContext(prepareMethod,
                new Object[]{null, intA, listB, arrayC, objD, objE, objF, g, h});
        System.out.println(paramContext);


        // int a, case: @BusinessActionContextParameter("a")
        Assertions.assertEquals(11, paramContext.get("a"));

        // list b, case: @BusinessActionContextParameter(paramName = "b", index = 0) + List
        Assertions.assertEquals("bb", paramContext.get("b"));

        // array c, case: @BusinessActionContextParameter(value = "c", index = 1) + Array
        Assertions.assertEquals(33L, paramContext.get("c"));

        // object d, case: @BusinessActionContextParameter(isParamInProperty = true)
        Assertions.assertEquals(1, paramContext.get("num")); // case: @BusinessActionContextParameter
        Assertions.assertFalse(paramContext.containsKey("name")); // case: no annotation
        Assertions.assertEquals("aaa@ali.com", paramContext.get("email0")); // case: @BusinessActionContextParameter(paramName = "email0")
        Assertions.assertFalse(paramContext.containsKey("remark")); // case: @BusinessActionContextParameter(paramName = "remark")
        Assertions.assertEquals("yes", paramContext.get("flag")); // case: @BusinessActionContextParameter(isParamInProperty = true, fetcher = MockBooleanParameterFetcher.class)

        // object e, case: @BusinessActionContextParameter(paramName = "e", isParamInProperty = true)
        // map
        Map<String, Object> contextE = (Map)paramContext.get("e");
        Assertions.assertNotNull(contextE);
        Assertions.assertEquals(3, contextE.size());
        // fields in map
        Assertions.assertEquals(2, contextE.get("num")); // case: @BusinessActionContextParameter
        Assertions.assertFalse(contextE.containsKey("name")); // case: no annotation
        Assertions.assertFalse(contextE.containsKey("email0")); // case: @BusinessActionContextParameter(paramName = "email0")
        Assertions.assertEquals("bbb is an IT man", contextE.get("remark")); // case: @BusinessActionContextParameter(paramName = "remark")
        Assertions.assertEquals("no", contextE.get("flag")); // case: @BusinessActionContextParameter(isParamInProperty = true, fetcher = MockBooleanParameterFetcher.class)

        // object f, case: @BusinessActionContextParameter(paramName = "f", isParamInProperty = true, fetcher = MockObjectParameterFetcher.class)
        // map
        Map<String, Object> contextF = (Map)paramContext.get("f");
        Assertions.assertNotNull(contextF);
        Assertions.assertEquals(2, contextF.size());
        // fields in map
        Assertions.assertEquals(3, contextF.get("num"));
        Assertions.assertEquals("ccc", contextF.get("name"));
        Assertions.assertFalse(contextF.containsKey("email0"));
        Assertions.assertFalse(contextF.containsKey("remark"));
        Assertions.assertFalse(contextF.containsKey("flag"));

        // boolean g, case: @BusinessActionContextParameter(paramName = "g", isParamInProperty = true, fetcher = MockBooleanParameterFetcher.class)
        Assertions.assertEquals("yes", paramContext.get("g"));

        // boolean h, case: @BusinessActionContextParameter(paramName = "h", isParamInProperty = true, fetcher = MockBooleanParameterFetcher.class)
        Assertions.assertEquals("no", paramContext.get("h"));
    }
}
