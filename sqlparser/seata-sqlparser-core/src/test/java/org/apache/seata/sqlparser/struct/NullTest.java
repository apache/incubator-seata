package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NullTest {

    @Test
    public void testGet() {
        Null instance = Null.get();
        assertEquals(instance, Null.get());
    }

    @Test
    public void testToString() {
        String expected = "NULL";
        assertEquals(expected.trim(), Null.get().toString().trim());
    }

}
