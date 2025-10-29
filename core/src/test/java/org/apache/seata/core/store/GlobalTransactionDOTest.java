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
package org.apache.seata.core.store;

import org.apache.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

public class GlobalTransactionDOTest {

    @Test
    public void testConstructorWithXid() {
        String xid = "192.168.1.1:8091:123456";
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO(xid);
        Assertions.assertEquals(xid, globalTransactionDO.getXid());
    }

    @Test
    public void testDefaultConstructor() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        Assertions.assertNotNull(globalTransactionDO);
    }

    @Test
    public void testSetAndGetXid() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        String xid = "192.168.1.1:8091:123456";
        globalTransactionDO.setXid(xid);
        Assertions.assertEquals(xid, globalTransactionDO.getXid());
    }

    @Test
    public void testSetAndGetTransactionId() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        long transactionId = 100L;
        globalTransactionDO.setTransactionId(transactionId);
        Assertions.assertEquals(transactionId, globalTransactionDO.getTransactionId());
    }

    @Test
    public void testSetAndGetTransactionIdWithLongObject() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        Long transactionId = 200L;
        globalTransactionDO.setTransactionId(transactionId);
        Assertions.assertEquals(transactionId, globalTransactionDO.getTransactionId());
    }

    @Test
    public void testSetAndGetStatus() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        int status = GlobalStatus.Begin.getCode();
        globalTransactionDO.setStatus(status);
        Assertions.assertEquals(status, globalTransactionDO.getStatus());
    }

    @Test
    public void testSetAndGetStatusWithInteger() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        Integer status = GlobalStatus.Committed.getCode();
        globalTransactionDO.setStatus(status);
        Assertions.assertEquals(status, globalTransactionDO.getStatus());
    }

    @Test
    public void testSetAndGetApplicationId() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        String applicationId = "test-app";
        globalTransactionDO.setApplicationId(applicationId);
        Assertions.assertEquals(applicationId, globalTransactionDO.getApplicationId());
    }

    @Test
    public void testSetAndGetTransactionServiceGroup() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        String serviceGroup = "default_tx_group";
        globalTransactionDO.setTransactionServiceGroup(serviceGroup);
        Assertions.assertEquals(serviceGroup, globalTransactionDO.getTransactionServiceGroup());
    }

    @Test
    public void testSetAndGetTransactionName() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        String transactionName = "test-transaction";
        globalTransactionDO.setTransactionName(transactionName);
        Assertions.assertEquals(transactionName, globalTransactionDO.getTransactionName());
    }

    @Test
    public void testSetAndGetTimeout() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        int timeout = 60000;
        globalTransactionDO.setTimeout(timeout);
        Assertions.assertEquals(timeout, globalTransactionDO.getTimeout());
    }

    @Test
    public void testSetAndGetTimeoutWithInteger() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        Integer timeout = 30000;
        globalTransactionDO.setTimeout(timeout);
        Assertions.assertEquals(timeout, globalTransactionDO.getTimeout());
    }

    @Test
    public void testSetAndGetBeginTime() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        long beginTime = System.currentTimeMillis();
        globalTransactionDO.setBeginTime(beginTime);
        Assertions.assertEquals(beginTime, globalTransactionDO.getBeginTime());
    }

    @Test
    public void testSetAndGetBeginTimeWithLongObject() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        Long beginTime = System.currentTimeMillis();
        globalTransactionDO.setBeginTime(beginTime);
        Assertions.assertEquals(beginTime, globalTransactionDO.getBeginTime());
    }

    @Test
    public void testSetAndGetApplicationData() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        String applicationData = "{\"key\":\"value\"}";
        globalTransactionDO.setApplicationData(applicationData);
        Assertions.assertEquals(applicationData, globalTransactionDO.getApplicationData());
    }

    @Test
    public void testSetAndGetGmtCreate() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        Date gmtCreate = new Date();
        globalTransactionDO.setGmtCreate(gmtCreate);
        Assertions.assertEquals(gmtCreate, globalTransactionDO.getGmtCreate());
    }

    @Test
    public void testSetAndGetGmtModified() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        Date gmtModified = new Date();
        globalTransactionDO.setGmtModified(gmtModified);
        Assertions.assertEquals(gmtModified, globalTransactionDO.getGmtModified());
    }

    @Test
    public void testToString() {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO("test-xid");
        globalTransactionDO.setTransactionId(100L);
        globalTransactionDO.setApplicationId("test-app");
        String result = globalTransactionDO.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("test-xid"));
    }
}
