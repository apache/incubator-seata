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
import io.seata.core.model.BranchType;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.RMClient;
import io.seata.rm.tcc.TCCResource;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * rm client test
 **/
public class RmClientTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RmClientTest.class);
    private static volatile DefaultResourceManager rm = null;

    private static DefaultResourceManager doGetRm(String resourceId) throws NoSuchMethodException {
        if (rm == null) {
            synchronized (RmClientTest.class) {
                if (rm == null) {
                    // register:TYPE_REG_RM = 103 , TYPE_REG_RM_RESULT = 104
                    RMClient.init(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP);

                    DefaultResourceManager resourceManager = DefaultResourceManager.get();
                    resourceManager
                            .getResourceManager(BranchType.TCC)
                            .getManagedResources()
                            .clear();

                    rm = resourceManager;
                    LOGGER.info("(0.6.1)RM init");
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
        tccResource.setCommitMethod(
                ReflectionUtil.getMethod(Action1.class, "commitTcc", new Class[] {BusinessActionContext.class}));
        tccResource.setRollbackMethod(
                ReflectionUtil.getMethod(Action1.class, "cancel", new Class[] {BusinessActionContext.class}));
        rm.registerResource(tccResource);
        LOGGER.info("(0.6.1)registerResource ok");
        return rm;
    }

    public static DefaultResourceManager getRm(String resourceId) throws NoSuchMethodException {
        int retry = 0;
        do {
            try {
                return doGetRm(resourceId);
            } catch (Exception e) {
                if (retry >= 2) {
                    throw e;
                }
                LOGGER.warn(" failed, retry times " + retry, e);
            }
            retry++;
        } while (true);
    }
}
