package org.apache.seata.sqlparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SQLTypeTest {

    @Test
    public void testValue() {
        assertEquals(0, SQLType.SELECT.value(), "SELECT value should be 0");
        assertEquals(1, SQLType.INSERT.value(), "INSERT value should be 1");
        // Add more assertions for other enum constants
    }

    @Test
    public void testValueOf() {
        assertEquals(SQLType.SELECT, SQLType.valueOf(0), "Should retrieve SELECT for value 0");
        assertEquals(SQLType.INSERT, SQLType.valueOf(1), "Should retrieve INSERT for value 1");
        // Add more assertions for other integer values
    }

    @Test
    public void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> SQLType.valueOf(100),
                "Should throw IllegalArgumentException for invalid value");
    }

}
