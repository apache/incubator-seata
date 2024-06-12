package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class IndexTypeTest {

    @Test
    public void testValue() {
        assertEquals(0, IndexType.PRIMARY.value(), "Value of PRIMARY index type should be 0");
        assertEquals(1, IndexType.NORMAL.value(), "Value of NORMAL index type should be 1");
        assertEquals(2, IndexType.UNIQUE.value(), "Value of UNIQUE index type should be 2");
        assertEquals(3, IndexType.FULL_TEXT.value(), "Value of FULL_TEXT index type should be 3");
    }

    @Test
    public void testValueOf() {
        assertEquals(IndexType.PRIMARY, IndexType.valueOf(0), "IndexType of value 0 should be PRIMARY");
        assertEquals(IndexType.NORMAL, IndexType.valueOf(1), "IndexType of value 1 should be NORMAL");
        assertEquals(IndexType.UNIQUE, IndexType.valueOf(2), "IndexType of value 2 should be UNIQUE");
        assertEquals(IndexType.FULL_TEXT, IndexType.valueOf(3), "IndexType of value 3 should be FULL_TEXT");
    }

    @Test
    public void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> IndexType.valueOf(4),
                "Should throw IllegalArgumentException for invalid value 4");
    }

}
