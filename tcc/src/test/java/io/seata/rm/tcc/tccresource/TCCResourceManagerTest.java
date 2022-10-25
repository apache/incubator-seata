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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSON;

import io.seata.common.Constants;
import io.seata.rm.tcc.TCCResource;
import io.seata.rm.tcc.TCCResourceManager;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * test getBusinessActionContext() in TCCResourceManager
 *
 * @author zouwei
 */
public class TCCResourceManagerTest {

    @Test
    public void testGetBusinessActionContext() {
        // 1.loadBusinessActionParamTypeFromPrepareMethod
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
        Map<String, Type> prepareParamTypeMap = tccResource.getPrepareParamTypeMap();
        // 2. Prepare test data
        Map<String, Object> dataMap = new HashMap<>();
        Map<String, Object> businessActionContextMap = new HashMap<>();
        businessActionContextMap.put("userId", "user_id_12345678");
        businessActionContextMap.put("amount", Long.valueOf(999L));
        businessActionContextMap.put("amountF", 0.99F);
        businessActionContextMap.put("amountD", Double.valueOf(8.88));
        businessActionContextMap.put("amountB", BigDecimal.valueOf(6.66));
        businessActionContextMap.put("sublistA", "sublistA");
        businessActionContextMap.put("sublistB", "sublistB");
        List<String> listC = new ArrayList<>();
        listC.add("list1");
        listC.add("list2");
        listC.add("list3");
        businessActionContextMap.put("listC", listC);
        businessActionContextMap.put("hello", 'Z');
        businessActionContextMap.put("array", "array");
        dataMap.put(Constants.TCC_ACTION_CONTEXT, businessActionContextMap);
        String applicationData = JSON.toJSONString(dataMap);
        // 3. test getBusinessActionContext
        TCCResourceManagerExt tccResourceManagerExt = new TCCResourceManagerExt();
        BusinessActionContext businessActionContext = tccResourceManagerExt.getBusinessActionContext("XID_123456",
            123456L, "RSD_123456", applicationData, prepareParamTypeMap);

        // 4.assert
        String userId = (String)businessActionContext.getActionContext("userId");
        Long amount = (Long)businessActionContext.getActionContext("amount");
        float amountF = (float)businessActionContext.getActionContext("amountF");
        Double amountD = (Double)businessActionContext.getActionContext("amountD");
        BigDecimal amountB = (BigDecimal)businessActionContext.getActionContext("amountB");
        String sublistA = (String)businessActionContext.getActionContext("sublistA");
        String sublistB = (String)businessActionContext.getActionContext("sublistB");
        List<String> listCFromAction = (List<String>)businessActionContext.getActionContext("listC");
        char hello = (char)businessActionContext.getActionContext("hello");
        String array = (String)businessActionContext.getActionContext("array");
        Assertions.assertEquals("user_id_12345678", userId);
        Assertions.assertEquals(Long.valueOf(999L), amount);
        Assertions.assertEquals(0.99F, amountF);
        Assertions.assertEquals(Double.valueOf(8.88), amountD);
        Assertions.assertEquals(BigDecimal.valueOf(6.66), amountB);
        Assertions.assertEquals("sublistA", sublistA);
        Assertions.assertEquals("sublistB", sublistB);
        Assertions.assertEquals(listC, listCFromAction);
        Assertions.assertEquals('Z', hello);
        Assertions.assertEquals("array", array);
    }

    private static class TCCResourceManagerExt extends TCCResourceManager {

        @Override
        public BusinessActionContext getBusinessActionContext(String xid, long branchId, String resourceId,
            String applicationData, Map<String, Type> prepareParamTypeMap) {
            return super.getBusinessActionContext(xid, branchId, resourceId, applicationData, prepareParamTypeMap);
        }
    }
}
