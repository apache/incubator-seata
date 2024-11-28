package org.apache.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href=mailto:ujjboy@qq.com>GengZhang</a>
 */
class UUIDGeneratorTest {

    @Test
    void generateUUID() {
        Assertions.assertTrue(UUIDGenerator.generateUUID() > 0);
    }
}