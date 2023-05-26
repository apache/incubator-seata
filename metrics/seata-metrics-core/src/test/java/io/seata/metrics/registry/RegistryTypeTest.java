package io.seata.metrics.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link RegistryType}
 *
 * @author Mia0451
 */
class RegistryTypeTest {

    @Test
    void getName() {
        Assertions.assertEquals("compact", RegistryType.COMPACT.getName());
    }

    @Test
    void getType_invalidTypeName_throwException() {
        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> RegistryType.getType("comp"));
    }

    @Test
    void getType_validTypeNameLowerCase() {
        Assertions.assertEquals(RegistryType.COMPACT, RegistryType.getType("compact"));
    }

    @Test
    void getType_validTypeNameMixedCase() {
        Assertions.assertEquals(RegistryType.COMPACT, RegistryType.getType("compAcT"));
    }

    @Test
    void values() {
        Assertions.assertArrayEquals(new RegistryType[]{RegistryType.COMPACT}, RegistryType.values());
    }
}
