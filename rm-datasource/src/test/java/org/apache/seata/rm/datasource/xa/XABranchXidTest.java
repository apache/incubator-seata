package org.apache.seata.rm.datasource.xa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for XABranchXid
 */
public class XABranchXidTest {
    @Test
    void testEquals() throws Exception {
        XABranchXid xid1 = new XABranchXid("xid1", 1);
        XABranchXid xid2 = new XABranchXid("xid1", 1);
        XABranchXid xid3 = new XABranchXid("xid2", 2);
        XABranchXid xid4 = null;

        Assertions.assertEquals(xid1, xid2);
        Assertions.assertNotEquals(xid1, xid3);
        Assertions.assertNotEquals(xid1, xid4);
        Assertions.assertNotEquals(xid1, new Object());
    }

    @Test
    void testHashCode() throws Exception {
        XABranchXid xid1 = new XABranchXid("xid1", 1);
        XABranchXid xid2 = new XABranchXid("xid1", 1);
        XABranchXid xid3 = new XABranchXid("xid2", 2);

        Assertions.assertEquals(xid1.hashCode(), xid2.hashCode());
        Assertions.assertNotEquals(xid1.hashCode(), xid3.hashCode());
    }
}
