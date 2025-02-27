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
package org.apache.seata.core.rpc.netty.mockserver;

import io.netty.channel.Channel;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.rpc.netty.ChannelManagerTestHelper;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.integration.tx.api.interceptor.ActionContextUtil;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.RMClient;
import org.apache.seata.rm.tcc.TCCResource;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * rm client test
 **/
public class RmClientTest {

    protected static final Logger LOGGER = LoggerFactory.getLogger(RmClientTest.class);

    public static void testRm() throws TransactionException {
        String resourceId = "mock-action";
        String xid = "1111";

        DefaultResourceManager rm = getRm(resourceId);

        //branchRegister:TYPE_BRANCH_REGISTER = 11 , TYPE_BRANCH_REGISTER_RESULT = 12
        Long branchId = rm.branchRegister(BranchType.AT, resourceId, "1", xid, "1", "1");
        Assertions.assertTrue(branchId > 0);


        // branchReport:TYPE_BRANCH_STATUS_REPORT = 13 , TYPE_BRANCH_STATUS_REPORT_RESULT = 14
        // TYPE_SEATA_MERGE = 59 , TYPE_SEATA_MERGE_RESULT = 60
        rm.branchReport(BranchType.AT, xid, branchId, BranchStatus.PhaseTwo_Committed, "");
        LOGGER.info("branchReport ok");

        //lockQuery:TYPE_GLOBAL_LOCK_QUERY = 21 , TYPE_GLOBAL_LOCK_QUERY_RESULT = 22
        RootContext.bind(xid);
        boolean b = rm.lockQuery(BranchType.AT, resourceId, xid, "1");
        LOGGER.info("lockQuery ok, result=" + b);
        Assertions.assertTrue(b);

        RmNettyRemotingClient remotingClient = RmNettyRemotingClient.getInstance();
        ConcurrentMap<String, Channel> channels = ChannelManagerTestHelper.getChannelConcurrentMap(remotingClient);
        channels.forEach(
                (key, value) -> RmNettyRemotingClient.getInstance().sendAsyncRequest(value, HeartbeatMessage.PING));

    }

    public static DefaultResourceManager getRm(String resourceId) {
        RMClient.init(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP);
        DefaultResourceManager rm = DefaultResourceManager.get();
        rm.getResourceManager(BranchType.TCC).getManagedResources().clear();

        //register:TYPE_REG_RM = 103 , TYPE_REG_RM_RESULT = 104
        Action1 target = new Action1Impl();
        registryTccResource(target);
        LOGGER.info("registerResource ok");
        return rm;
    }

    /**
     * only compatible history ut
     * TODO fix
     */
    @Deprecated
    private static void registryTccResource(Action1 target) {
        Map<Method, Class<?>> matchMethodClazzMap = ReflectionUtil.findMatchMethodClazzMap(target.getClass(), method -> method.isAnnotationPresent(TwoPhaseBusinessAction.class));
        if (matchMethodClazzMap.keySet().isEmpty()) {
            return;
        }

        try {
            for (Map.Entry<Method, Class<?>> methodClassEntry : matchMethodClazzMap.entrySet()) {
                Method method = methodClassEntry.getKey();
                Class<?> methodClass = methodClassEntry.getValue();

                TwoPhaseBusinessAction twoPhaseBusinessAction = method.getAnnotation(TwoPhaseBusinessAction.class);
                TCCResource tccResource = new TCCResource();
                if (StringUtils.isBlank(twoPhaseBusinessAction.name())) {
                    throw new FrameworkException("TCC bean name cannot be null or empty");
                }
                tccResource.setActionName(twoPhaseBusinessAction.name());
                tccResource.setTargetBean(target);
                tccResource.setPrepareMethod(method);
                tccResource.setCommitMethodName(twoPhaseBusinessAction.commitMethod());
                tccResource.setCommitMethod(methodClass.getMethod(twoPhaseBusinessAction.commitMethod(),
                        twoPhaseBusinessAction.commitArgsClasses()));
                tccResource.setRollbackMethodName(twoPhaseBusinessAction.rollbackMethod());
                tccResource.setRollbackMethod(methodClass.getMethod(twoPhaseBusinessAction.rollbackMethod(),
                        twoPhaseBusinessAction.rollbackArgsClasses()));
                // set argsClasses
                tccResource.setCommitArgsClasses(twoPhaseBusinessAction.commitArgsClasses());
                tccResource.setRollbackArgsClasses(twoPhaseBusinessAction.rollbackArgsClasses());
                // set phase two method's keys
                tccResource.setPhaseTwoCommitKeys(ActionContextUtil.getTwoPhaseArgs(tccResource.getCommitMethod(),
                        twoPhaseBusinessAction.commitArgsClasses()));
                tccResource.setPhaseTwoRollbackKeys(ActionContextUtil.getTwoPhaseArgs(tccResource.getRollbackMethod(),
                        twoPhaseBusinessAction.rollbackArgsClasses()));
                DefaultResourceManager.get().registerResource(tccResource);
            }
        } catch (Throwable t) {
            throw new FrameworkException(t, "register tcc resource error");
        }
    }


}
