package org.apache.seata.tm.api.transaction;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SuspendedResourcesHolderTest {

    private final static String DEFAULT_XID = "1234567890";

    @Test
    void testIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new SuspendedResourcesHolder(null);
        });
    }

    @Test
    void getXidTest() {
        SuspendedResourcesHolder suspendedResourcesHolder = new SuspendedResourcesHolder(DEFAULT_XID);
        Assertions.assertEquals(DEFAULT_XID, suspendedResourcesHolder.getXid());
    }
}
