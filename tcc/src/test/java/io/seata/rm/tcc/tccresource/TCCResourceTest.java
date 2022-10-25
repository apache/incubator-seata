/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.tcc.tccresource;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.seata.rm.tcc.TCCResource;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 *
 *
 * @author zouwei
 */
public class TCCResourceTest {

    /**
     * test prepareMethod is null
     */
    @Test
    public void testLoadBusinessActionParamTypeFromNull() {
        TCCResource tccResource = new TCCResource();
        tccResource.setPrepareMethod(null);
        tccResource.loadBusinessActionParamTypeFromPrepareMethod();
        Assertions.assertNull(tccResource.getPrepareParamTypeMap());
    }

    /**
     * test prepareMethod is null TccAction#prepare()
     */
    @Test
    public void testLoadBusinessActionParamTypeFromPrepareMethod() {
        Class<?> clazz = IWalletTccAction.class;
        Method[] methods = clazz.getDeclaredMethods();
        Method prepareMethod = null;
        for (Method m : methods) {
            if (m.isAnnotationPresent(TwoPhaseBusinessAction.class)) {
                prepareMethod = m;
            }
        }
        TCCResource tccResource = new TCCResource();
        tccResource.setPrepareMethod(prepareMethod);
        tccResource.loadBusinessActionParamTypeFromPrepareMethod();
        Map<String, Type> paramTypeMap = tccResource.getPrepareParamTypeMap();
        Assertions.assertEquals(String.class, paramTypeMap.get("userId"));
        Assertions.assertEquals(Long.class, paramTypeMap.get("amount"));
        Assertions.assertEquals(float.class, paramTypeMap.get("amountF"));
        Assertions.assertEquals(Double.class, paramTypeMap.get("amountD"));
        Assertions.assertEquals(BigDecimal.class, paramTypeMap.get("amountB"));
        // List is not ParameterizedType, type of sub element default is Object.class, No need to get the type of
        // parameter b
        Assertions.assertNull(paramTypeMap.get("sublistA"));
        Assertions.assertEquals(String.class, paramTypeMap.get("sublistB"));

        Type type = paramTypeMap.get("listC");
        Assertions.assertEquals("java.util.List<java.lang.String>", type.getTypeName());
        Assertions.assertEquals(char.class, paramTypeMap.get("hello"));
        Assertions.assertEquals(Integer.class, paramTypeMap.get("num"));
        Assertions.assertEquals(String.class, paramTypeMap.get("array"));
    }
}
