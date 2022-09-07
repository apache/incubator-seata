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