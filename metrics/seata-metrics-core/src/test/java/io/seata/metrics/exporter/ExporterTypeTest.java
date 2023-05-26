package io.seata.metrics.exporter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ExporterType}
 *
 * @author Mia0451
 */
class ExporterTypeTest {

    @Test
    void values() {
        Assertions.assertArrayEquals(new ExporterType[]{ExporterType.PROMETHEUS}, ExporterType.values());
    }

    @Test
    void getName() {
        Assertions.assertEquals("prometheus", ExporterType.PROMETHEUS.getName());
    }

    @Test
    void getType_invalidTypeName_throwException() {
        Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> ExporterType.getType("foo"));
    }

    @Test
    void getType_validTypeNameLowerCase() {
        Assertions.assertEquals(ExporterType.PROMETHEUS, ExporterType.getType("prometheus"));
    }

    @Test
    void getType_validTypeNameMixedCase() {
        Assertions.assertEquals(ExporterType.PROMETHEUS, ExporterType.getType("proMethEus"));
    }
}
