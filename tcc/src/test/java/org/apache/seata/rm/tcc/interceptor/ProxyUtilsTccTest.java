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
package org.apache.seata.rm.tcc.interceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.integration.tx.api.util.ProxyUtil;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.tcc.NormalTccAction;
import org.apache.seata.rm.tcc.NormalTccActionImpl;
import org.apache.seata.rm.tcc.TccParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ProxyUtilsTccTest {

    private final String DEFAULT_XID = "default_xid";

    AtomicReference<String> branchReference = new AtomicReference<String>();


    ResourceManager resourceManager = new ResourceManager() {

        @Override
        public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {
            branchReference.set(resourceId);
            return System.currentTimeMillis();
        }

        @Override
        public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {

        }

        @Override
        public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
            return false;
        }

        @Override
        public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
            return null;
        }

        @Override
        public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
            return null;
        }

        @Override
        public void registerResource(Resource resource) {

        }

        @Override
        public void unregisterResource(Resource resource) {

        }

        @Override
        public Map<String, Resource> getManagedResources() {
            return null;
        }

        @Override
        public BranchType getBranchType() {
            return null;
        }
    };


    @Test
    public void testTcc() {
        //given
        NormalTccActionImpl tccAction = new NormalTccActionImpl();
        NormalTccAction tccActionProxy = ProxyUtil.createProxy(tccAction);
        RootContext.bind(DEFAULT_XID);

        TccParam tccParam = new TccParam(1, "abc@163.com");
        List<String> listB = Arrays.asList("b");

        DefaultResourceManager.mockResourceManager(BranchType.TCC, resourceManager);

        //when
        String result = tccActionProxy.prepare(null, 0, listB, tccParam);

        //then
        Assertions.assertEquals("a", result);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("tccActionForTest", branchReference.get());
    }

    @Test
    public void testTccThrowRawException() {
        //given
        NormalTccActionImpl tccAction = new NormalTccActionImpl();
        NormalTccAction tccActionProxy = ProxyUtil.createProxy(tccAction);
        RootContext.bind(DEFAULT_XID);

        TccParam tccParam = new TccParam(1, "abc@163.com");
        List<String> listB = Arrays.asList("b");

        DefaultResourceManager.mockResourceManager(BranchType.TCC, resourceManager);

        //when
        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> tccActionProxy.prepareWithException(null, 0, listB, tccParam));
    }

    @Test
    public void testTccImplementOtherMethod(){
        NormalTccActionImpl tccAction = new NormalTccActionImpl();
        NormalTccActionImpl tccActionProxy = ProxyUtil.createProxy(tccAction);

        Assertions.assertTrue(tccActionProxy.otherMethod());

    }


}
