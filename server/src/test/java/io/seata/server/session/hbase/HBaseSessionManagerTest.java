package io.seata.server.session.hbase;

import io.seata.common.XID;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.hbase.session.HBaseSessionManager;
import io.seata.server.storage.hbase.store.HBaseTransactionStoreManager;
import org.apache.catalina.core.ApplicationContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ClassName: HBaseSessionManagerTest
 * Description: The hbase session manager test.
 *
 * @author haishin
 */
@SpringBootTest
public class HBaseSessionManagerTest {
    static SessionManager sessionManager = null;

    static Connection connection = null;


    @BeforeAll
    public static void start() throws Exception {
        HBaseSessionManager tempSessionManager = new HBaseSessionManager();
        HBaseTransactionStoreManager transactionStoreManager = HBaseTransactionStoreManager.getInstance();

        Configuration configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum","hadoop1");

        connection = ConnectionFactory.createConnection(configuration);

        transactionStoreManager.setConnection(connection);
        transactionStoreManager.setTableName("seata:table");
        transactionStoreManager.setStatusTableName("seata:statusTable");
        transactionStoreManager.setGlobalCF("globalCF");
        transactionStoreManager.setBranchesCF("branchesCF");
        transactionStoreManager.setTransactionIdCF("transactionIdCF");

        tempSessionManager.setTransactionStoreManager(transactionStoreManager);
        sessionManager = tempSessionManager;
    }

    @Test
    public void test_addGlobalSession() throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(session.getTransactionId());
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session);

        sessionManager.removeGlobalSession(session);
    }

}
