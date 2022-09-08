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
package io.seata.server.storage.mq.kafka;

import io.seata.common.ConfigurationKeys;
import io.seata.common.XID;
import io.seata.core.model.BranchType;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.producer.MqProducerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;

@Disabled
@SpringBootTest
class MqProducerFactoryTest {

    @Test
    void globalSessionPublishTest() {
        GlobalSession globalSession = GlobalSession.createGlobalSession("demo-app", "default_tx_group",
                "tx-1", 3000);
        MqProducerFactory.getInstance().publish(ConfigurationKeys.STORE_DB_GLOBAL_TABLE, globalSession.getXid().getBytes(StandardCharsets.UTF_8), globalSession.encode());
    }

    @Test
    void branchSessionPublishTest() {
        GlobalSession globalSession = GlobalSession.createGlobalSession("demo-app", "default_tx_group", "tx-1",
                3000);
        globalSession.setXid(XID.generateXID(globalSession.getTransactionId()));
        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId(DEFAULT_TX_GROUP);
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        MqProducerFactory.getInstance().publish(ConfigurationKeys.STORE_DB_BRANCH_TABLE, branchSession.getXid().getBytes(StandardCharsets.UTF_8), branchSession.encode());
    }
}