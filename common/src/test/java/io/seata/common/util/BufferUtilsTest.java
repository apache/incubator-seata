package io.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

/**
 * @description:
 * @author: zhangjiawei
 * @date: 2023/12/18
 */
public class BufferUtilsTest {

    @Test
    public void testFlip() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.flip(byteBuffer));
    }

    @Test
    public void testClear() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.clear(byteBuffer));
    }

    @Test
    public void testLimit() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.limit(byteBuffer, 4));
    }

    @Test
    public void testMark() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.mark(byteBuffer));
    }

    @Test
    public void testPosition() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.position(byteBuffer, 0));
    }

    @Test
    public void testRewind() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        Assertions.assertDoesNotThrow(() -> BufferUtils.rewind(byteBuffer));
    }

    @Test
    public void testReset() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(1);
        BufferUtils.mark(byteBuffer);
        Assertions.assertDoesNotThrow(() -> BufferUtils.reset(byteBuffer));
    }
}
