package com.alibaba.fescar.core.protocol;

import com.alibaba.fescar.core.protocol.AbstractMessage;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AbstractMessageTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void bytesToIntOffsetZero() {
    final byte[] bytes = {11, 61, 19, 111}; 
    final int offset = 0;
    final int retval = AbstractMessage.bytesToInt(bytes, offset);
    Assert.assertEquals((11 << 24) + (61 << 16) + (19 << 8) + 111, retval);
  }

  @Test
  public void bytesToIntOffsetZeroTruncated() {
    final byte[] bytes = {0, 10}; 
    final int offset = 0;
    final int retval = AbstractMessage.bytesToInt(bytes, offset);
    Assert.assertEquals(10, retval);
  }

  @Test
  public void bytesToIntOffsetNegativeArrayIndexOutOfBoundsException() {
    final byte[] bytes = {0};
    final int offset = -1;
    thrown.expect(ArrayIndexOutOfBoundsException.class);
    AbstractMessage.bytesToInt(bytes, offset);
  }

  @Test
  public void bytesToIntOffsetPositive() {
    final byte[] bytes = {5, 6, 12, 71, 89, 3};
    final int offset = 2;
    final int retval = AbstractMessage.bytesToInt(bytes, offset);
    Assert.assertEquals((12 << 24) + (71 << 16) + (89 << 8) + 3, retval);
  }

  @Test
  public void intToBytesOffsetZero() {
    final byte[] bytes = new byte[4];
    final int offset = 0;
    AbstractMessage.intToBytes((23 << 24) + (52 << 16) + (6 << 8) + 9, bytes, offset);
    Assert.assertArrayEquals(bytes, new byte[] {23, 52, 6, 9});
  }

  @Test
  public void intToBytesOffsetPositive() {
    final byte[] bytes = {17, 17, 17, 17, 17, 17};
    final int offset = 1;
    AbstractMessage.intToBytes((3 << 24) + (8 << 16) + (1 << 8) + 5, bytes, offset);
    Assert.assertArrayEquals(bytes, new byte[] {17, 3, 8, 1, 5, 17});
  }

  @Test
  public void intToBytesOffsetNegativeArrayIndexOutOfBoundsException() {
    final byte[] bytes = new byte[4];
    final int offset = -1;
    thrown.expect(ArrayIndexOutOfBoundsException.class);
    AbstractMessage.intToBytes(0, bytes, offset);
  }
}
