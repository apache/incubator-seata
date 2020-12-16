package io.seata.tm;

import io.seata.core.rpc.netty.TmNettyRemotingClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * test for tmClient
 */
public class TMClientTest {

    private static final String APPLICATION_ID = "my_app_test";
    private static final String SERVICE_GROUP = "my_test_tx_group";

    @Test
    public void testInit(){
        TMClient.init(APPLICATION_ID,SERVICE_GROUP,null,null);
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance();
        Assertions.assertEquals(tmNettyRemotingClient.getTransactionServiceGroup(),SERVICE_GROUP);
    }
}
