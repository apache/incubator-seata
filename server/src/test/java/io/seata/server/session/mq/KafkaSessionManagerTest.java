package io.seata.server.session.mq;

import io.seata.common.ConfigurationKeys;
import io.seata.common.XID;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.mq.session.kafka.KafkaSessionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;

@SpringBootTest
class KafkaSessionManagerTest {

    @Test
    void globalSessionPublishTest() {
        // TODO need mock a kafka server
        GlobalSession globalSession = GlobalSession.createGlobalSession("demo-app", "default_tx_group",
                "tx-1", 3000);
        try {
            KafkaSessionManager.getInstance().publish(ConfigurationKeys.STORE_DB_GLOBAL_TABLE, globalSession.encode());
        } catch (TransactionException e) {
            Assertions.fail();
        }
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
        try {
            KafkaSessionManager.getInstance().publish(ConfigurationKeys.STORE_DB_BRANCH_TABLE, branchSession.encode());
        } catch (TransactionException e) {
            Assertions.fail();
        }
    }
}