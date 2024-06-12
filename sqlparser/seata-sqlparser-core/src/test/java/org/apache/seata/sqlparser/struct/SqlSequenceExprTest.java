package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SqlSequenceExprTest {

    @Test
    public void testDefaultConstructor() {

        SqlSequenceExpr expr = new SqlSequenceExpr();

        assertNull(expr.getSequence(), "Initial sequence should be null.");
        assertNull(expr.getFunction(), "Initial function should be null.");
    }

    @Test
    public void testParameterizedConstructor() {

        SqlSequenceExpr expr = new SqlSequenceExpr("mySequence", "myFunction");

        assertEquals("mySequence", expr.getSequence(), "Sequence should be 'mySequence'.");
        assertEquals("myFunction", expr.getFunction(), "Function should be 'myFunction'.");
    }

    @Test
    public void testSetGetSequence() {

        SqlSequenceExpr expr = new SqlSequenceExpr();

        expr.setSequence("newSequence");
        assertEquals("newSequence", expr.getSequence(), "Sequence should be 'newSequence'.");
    }

    @Test
    public void testSetGetFunction() {

        SqlSequenceExpr expr = new SqlSequenceExpr();

        expr.setFunction("newFunction");
        assertEquals("newFunction", expr.getFunction(), "Function should be 'newFunction'.");
    }

}
