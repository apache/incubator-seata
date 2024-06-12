package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SqlMethodExprTest {
    @Test
    public void testGet() {
        SqlMethodExpr instance = SqlMethodExpr.get();
        assertEquals(instance, SqlMethodExpr.get());
    }

    @Test
    public void testToString() {
        String expected = "SQL_METHOD";
        assertEquals(expected.trim(), SqlMethodExpr.get().toString().trim());
    }

}
