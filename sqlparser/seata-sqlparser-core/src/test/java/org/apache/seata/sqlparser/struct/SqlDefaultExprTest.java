package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class SqlDefaultExprTest {
    @Test
    public void testGet() {
        SqlDefaultExpr instance = SqlDefaultExpr.get();
        assertEquals(instance, SqlDefaultExpr.get());
    }

    @Test
    public void testToString() {
        String expected = "DEFAULT";
        assertEquals(expected.trim(), SqlDefaultExpr.get().toString().trim());
    }
}
