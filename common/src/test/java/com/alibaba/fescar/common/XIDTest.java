package com.alibaba.fescar.common;

import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author melon.zhao
 * @since 2019/2/21
 */
public class XIDTest {

    @Test
    public void testSetIpAddress() {
        XID.setIpAddress("127.0.0.1");
        Assert.assertEquals(XID.getIpAddress(), "127.0.0.1");
    }

    @Test
    void testSetPort() {
        XID.setPort(8080);
        Assert.assertEquals(XID.getPort(), 8080);
    }


    @Test
    void testGenerateXID() {
        long tranId = new Random().nextLong();
        XID.setPort(8080);
        XID.setIpAddress("127.0.0.1");
        Assert.assertEquals(XID.generateXID(tranId), XID.getIpAddress() + ":" + XID.getPort() + ":" + tranId);
    }

    @Test
    void testGetServerAddress() {
        Assert.assertNull(XID.getServerAddress(null));
        Assert.assertEquals(XID.getServerAddress("127.0.0.1:8080:8577662204289747564"), "127.0.0.1:8080");
    }

    @Test
    void testGetTransactionId() {
        Assert.assertEquals(XID.getTransactionId(null), -1);
        Assert.assertEquals(XID.getTransactionId("127.0.0.1:8080:8577662204289747564"),8577662204289747564L);
    }
}
