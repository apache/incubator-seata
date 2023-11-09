package io.seata.server.raft;

import io.seata.common.ConfigurationKeys;
import io.seata.common.XID;
import io.seata.server.cluster.raft.RaftServerFactory;
import io.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
public class RaftServerTest {

    @BeforeAll
    public static void setUp(ApplicationContext context) {}

    @AfterAll
    public static void destroy() {
        RaftServerFactory.getInstance().destroy();
        System.setProperty("server.raftPort", "0");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR, "");
        StoreConfig.setStartupParameter("file", "file", "file");
    }

    @Test
    public void initRaftServerFail() {
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertThrows(IllegalArgumentException.class, () -> RaftServerFactory.getInstance().init());
    }

    @Test
    public void initRaftServerFailByRaftPortNull() {
        System.setProperty("server.raftPort", "0");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
            XID.getIpAddress() + ":9091" + "," + XID.getIpAddress() + ":9092" + "," + XID.getIpAddress() + ":9093");
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertThrows(IllegalArgumentException.class, () -> RaftServerFactory.getInstance().init());
    }

    @Test
    public void initRaftServerStart() {
        System.setProperty("server.raftPort", "9091");
        System.setProperty(ConfigurationKeys.SERVER_RAFT_SERVER_ADDR,
            XID.getIpAddress() + ":9091" + "," + XID.getIpAddress() + ":9092" + "," + XID.getIpAddress() + ":9093");
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        Assertions.assertDoesNotThrow(() -> RaftServerFactory.getInstance().init());
        Assertions.assertNotNull(RaftServerFactory.getInstance().getRaftServer("default"));
        RaftServerFactory.getInstance().start();
    }

}
