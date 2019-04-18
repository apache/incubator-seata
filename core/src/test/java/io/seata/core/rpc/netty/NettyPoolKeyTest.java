package io.seata.core.rpc.netty;

import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.rpc.netty.NettyPoolKey.TransactionRole;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author: jimin.jm@alibaba-inc.com
 * @date 2019/04/12
 */
public class NettyPoolKeyTest {

    private NettyPoolKey nettyPoolKey;
    private static final NettyPoolKey.TransactionRole RM_ROLE = NettyPoolKey.TransactionRole.RMROLE;
    private static final NettyPoolKey.TransactionRole TM_ROLE = NettyPoolKey.TransactionRole.TMROLE;
    private static final String ADDRESS1 = "127.0.0.1:8091";
    private static final String ADDRESS2 = "127.0.0.1:8092";
    private static final RegisterRMRequest MSG1 = new RegisterRMRequest("applicationId1", "transactionServiceGroup1");
    private static final RegisterRMRequest MSG2 = new RegisterRMRequest("applicationId2", "transactionServiceGroup2");

    @Before
    public void init() {
        nettyPoolKey = new NettyPoolKey(RM_ROLE, ADDRESS1, MSG1);
    }

    @Test
    public void getTransactionRole() {
        Assert.assertEquals(nettyPoolKey.getTransactionRole(), RM_ROLE);
    }

    @Test
    public void setTransactionRole() {
        nettyPoolKey.setTransactionRole(TM_ROLE);
        Assert.assertEquals(nettyPoolKey.getTransactionRole(), TM_ROLE);
    }

    @Test
    public void getAddress() {
        Assert.assertEquals(nettyPoolKey.getAddress(), ADDRESS1);
    }

    @Test
    public void setAddress() {
        nettyPoolKey.setAddress(ADDRESS2);
        Assert.assertEquals(nettyPoolKey.getAddress(), ADDRESS2);
    }

    @Test
    public void getMessage() {
        Assert.assertEquals(nettyPoolKey.getMessage(), MSG1);
    }

    @Test
    public void setMessage() {
        nettyPoolKey.setMessage(MSG2);
        Assert.assertEquals(nettyPoolKey.getMessage(), MSG2);
    }

    @Test
    public void testToString() {
        String expectStr = "transactionRole:RMROLE,address:127.0.0.1:8091,msg:< " + MSG1.toString() + " >";
        Assert.assertEquals(nettyPoolKey.toString(), expectStr);
    }
}