package io.seata.common.util;

import io.seata.common.BranchDO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 * @author : MentosL
 * @date : 2023/3/2 13:59
 */
public class BitUtilsTest {

    @Test
    public void testSetBit() {
        int bitTag = 1 << 3;
        int i = 4;
        Assertions.assertEquals(BitUtils.setBit(i,bitTag),12);
    }


    @Test
    public void testUnSetBit() {
        int bitTag = 1 << 3;
        int i = 12;
        Assertions.assertEquals(BitUtils.unSetBit(i,bitTag),4);
    }


    @Test
    public void testIsSetBit() {
        int bitTag = 1 << 3;
        int i = 12;
        Assertions.assertTrue(BitUtils.isSetBit(i,bitTag));
    }

}
