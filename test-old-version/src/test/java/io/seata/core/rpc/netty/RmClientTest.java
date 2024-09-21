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
package io.seata.core.rpc.netty;

import io.seata.common.util.ReflectionUtil;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.RMClient;
import io.seata.rm.tcc.TCCResource;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * rm client test
 **/
public class RmClientTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RmClientTest.class);
    private static DefaultResourceManager rm = null;

    public static void testRm(String resourceId) throws TransactionException, NoSuchMethodException {
        String xid = "1111";

        DefaultResourceManager rm = getRm(resourceId);

        //branchRegister:TYPE_BRANCH_REGISTER = 11 , TYPE_BRANCH_REGISTER_RESULT = 12
        Long branchId = rm.branchRegister(BranchType.AT, resourceId, "1", xid, "1", "1");
        Assertions.assertTrue(branchId > 0);


        // (not support)branchReport:TYPE_BRANCH_STATUS_REPORT = 13 , TYPE_BRANCH_STATUS_REPORT_RESULT = 14
        // (not support)lockQuery:TYPE_GLOBAL_LOCK_QUERY = 21 , TYPE_GLOBAL_LOCK_QUERY_RESULT = 22

    }


    public static DefaultResourceManager getRm(String resourceId) throws NoSuchMethodException {
        if (rm == null) {
            synchronized (RmClientTest.class) {
                if (rm == null) {
                    //register:TYPE_REG_RM = 103 , TYPE_REG_RM_RESULT = 104
                    RMClient.init(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP);

                    DefaultResourceManager resourceManager = DefaultResourceManager.get();
                    resourceManager.getResourceManager(BranchType.TCC).getManagedResources().clear();

                    rm = resourceManager;
                }
            }
        }
        Action1 target = new Action1Impl();
        TCCResource tccResource = new TCCResource();
        tccResource.setActionName(resourceId);
        tccResource.setTargetBean(target);
        tccResource.setPrepareMethod(target.getClass().getMethod("insert", Long.class, Map.class));
        tccResource.setCommitMethodName("commitTcc");
        tccResource.setRollbackMethodName("cancel");
        tccResource.setCommitMethod(ReflectionUtil.getMethod(Action1.class, "commitTcc", new Class[]{BusinessActionContext.class}));
        tccResource.setRollbackMethod(ReflectionUtil.getMethod(Action1.class, "cancel", new Class[]{BusinessActionContext.class}));
        rm.registerResource(tccResource);
        LOGGER.info("registerResource ok");
        return rm;
    }


}
