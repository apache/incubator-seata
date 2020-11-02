package io.seata.common.rpc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The state statistics test.
 *
 * @author ph3636
 */
public class RpcStatusTest {

    public static final String SERVICE = "127.0.0.1:80";

    @Test
    public void getStatus() {
        RpcStatus rpcStatus1 = RpcStatus.getStatus(SERVICE);
        Assertions.assertNotNull(rpcStatus1);
        RpcStatus rpcStatus2 = RpcStatus.getStatus(SERVICE);
        Assertions.assertEquals(rpcStatus1, rpcStatus2);
    }

    @Test
    public void removeStatus() {
        RpcStatus old = RpcStatus.getStatus(SERVICE);
        RpcStatus.removeStatus(SERVICE);
        Assertions.assertNotEquals(RpcStatus.getStatus(SERVICE), old);
    }

    @Test
    public void beginCount() {
        RpcStatus.beginCount(SERVICE);
        Assertions.assertEquals(RpcStatus.getStatus(SERVICE).getActive(), 1);
    }

    @Test
    public void endCount() {
        RpcStatus.endCount(SERVICE);
        Assertions.assertEquals(RpcStatus.getStatus(SERVICE).getActive(), 0);
        Assertions.assertEquals(RpcStatus.getStatus(SERVICE).getTotal(), 1);
    }
}
