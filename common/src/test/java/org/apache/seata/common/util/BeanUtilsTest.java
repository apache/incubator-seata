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
package org.apache.seata.common.util;

import org.apache.seata.common.BranchDO;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.rpc.RpcStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The bean utils test
 *
 */
public class BeanUtilsTest {

    @Test
    public void testInit() {
        BeanUtils beanUtils = new BeanUtils();
        Assertions.assertNotNull(beanUtils);
    }

    @Test
    public void testBeanToString() {
        BranchDO branchDO = new BranchDO("xid123123", 123L, 1, 2.2, new Date());
        Assertions.assertNotNull(BeanUtils.beanToString(branchDO));
        // null object
        Assertions.assertNull(BeanUtils.beanToString(null));
        // buffer length < 2
        Assertions.assertNotNull(BeanUtils.beanToString(new Object()));
        // null val
        Assertions.assertNotNull(BeanUtils.beanToString(new BranchDO(null, null, null, null, null)));
    }

    @Test
    public void testMapToObject() {
        // null map
        BranchDO branchDO =
                (BranchDO) BeanUtils.mapToObject(null, BranchDO.class);
        Assertions.assertNull(branchDO);

        Map<String, String> map = new HashMap<>();
        Date date = new Date();
        map.put("xid", "192.166.166.11:9010:12423424234234");
        map.put("transactionId", "12423424234234");
        map.put("status", "2");
        map.put("test", "22.22");
        map.put("gmtCreate", String.valueOf(date.getTime()));
        map.put("msg", "test");
        map.put("testByte", "1");
        branchDO = (BranchDO) BeanUtils.mapToObject(map, BranchDO.class);
        Assertions.assertEquals(map.get("xid"), branchDO.getXid());
        Assertions.assertEquals(Long.valueOf(map.get("transactionId")), branchDO.getTransactionId());
        Assertions.assertEquals(Integer.valueOf(map.get("status")), branchDO.getStatus());
        Assertions.assertEquals(Double.valueOf(map.get("test")), branchDO.getTest());
        Assertions.assertEquals(new Date(date.getTime()), branchDO.getGmtCreate());

        map = new HashMap<>();
        map.put("xid", null);
        map.put("transactionId", null);
        map.put("status", null);
        map.put("test", null);
        map.put("gmtCreate", null);
        branchDO = (BranchDO) BeanUtils.mapToObject(map, BranchDO.class);
        Assertions.assertNull(branchDO.getXid());
        Assertions.assertNull(branchDO.getTransactionId());
        Assertions.assertNull(branchDO.getStatus());
        Assertions.assertNull(branchDO.getTest());
        Assertions.assertNull(branchDO.getGmtCreate());
        // InstantiationException
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            Map<String, String> map1 = new HashMap<>();
            map1.put("xid", "1");
            BeanUtils.mapToObject(map1, DefaultValues.class);
        });
        // IllegalAccessException
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            Map<String, String> map1 = new HashMap<>();
            map1.put("xid", "1");
            BeanUtils.mapToObject(map1, RpcStatus.class);
        });
    }

    @Test
    public void testObjectToMap() {
        BranchDO branchDO = new BranchDO("xid123123", 123L, 1, 2.2, new Date());
        Map<String, String> map = BeanUtils.objectToMap(branchDO);
        Assertions.assertEquals(branchDO.getXid(), map.get("xid"));
        Assertions.assertEquals(branchDO.getTransactionId(), Long.valueOf(map.get("transactionId")));
        Assertions.assertEquals(branchDO.getStatus(), Integer.valueOf(map.get("status")));
        Assertions.assertEquals(branchDO.getTest(), Double.valueOf(map.get("test")));
        Assertions.assertEquals(branchDO.getGmtCreate().getTime(),Long.valueOf(map.get("gmtCreate")));

        Assertions.assertNull(BeanUtils.objectToMap(null));

        // date is null / field is null
        branchDO = new BranchDO("xid123123", null, 1, 2.2, null);
        map = BeanUtils.objectToMap(branchDO);
        Assertions.assertNull(map.get("gmtCreate"));
        Assertions.assertEquals("", map.get("transactionId"));
    }
}
