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
package io.seata.core.rpc.netty;

import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.integration.tx.api.interceptor.parser.DefaultResourceRegisterParser;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.RMClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * rm client test
 *
 * @author minghua.xie
 * @date 2023/11/21
 **/
public class RmClientTestV1 {
    protected static final Logger LOGGER = LoggerFactory.getLogger(RmClientTestV1.class);
    public static void main(String[] args) throws TransactionException {
        RMClient.init(MockConstants.APPLICATION_ID, MockConstants.SERVICE_GROUP);
        DefaultResourceManager rm = DefaultResourceManager.get();

        //register:TYPE_REG_RM = 103 , TYPE_REG_RM_RESULT = 104
        String rid = "mock-action";
        Action1 target = new Action1Impl();
        DefaultResourceRegisterParser.get().registerResource(target, rid);
        LOGGER.info("registerResource ok");

        //branchRegister:TYPE_BRANCH_REGISTER = 11 , TYPE_BRANCH_REGISTER_RESULT = 12
        Long branchId = rm.branchRegister(BranchType.AT, rid, "1", "1", "1", "1");
        LOGGER.info("branchRegister ok, branchId=" + branchId);


        // branchReport:TYPE_BRANCH_STATUS_REPORT = 13 , TYPE_BRANCH_STATUS_REPORT_RESULT = 14
        // TYPE_SEATA_MERGE = 59 , TYPE_SEATA_MERGE_RESULT = 60
        rm.branchReport(BranchType.AT, "1", branchId, BranchStatus.PhaseTwo_Committed, "");
        LOGGER.info("branchReport ok");

        //lockQuery:TYPE_GLOBAL_LOCK_QUERY = 21 , TYPE_GLOBAL_LOCK_QUERY_RESULT = 22
        RootContext.bind("1");
        boolean b = rm.lockQuery(BranchType.AT, rid, "1", "1");
        LOGGER.info("lockQuery ok, result=" + b);

        RmNettyRemotingClient remotingClient = RmNettyRemotingClient.getInstance();
        remotingClient.getClientChannelManager().getChannels().forEach(
                (key, value) -> RmNettyRemotingClient.getInstance().sendAsyncRequest(value, HeartbeatMessage.PING));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
