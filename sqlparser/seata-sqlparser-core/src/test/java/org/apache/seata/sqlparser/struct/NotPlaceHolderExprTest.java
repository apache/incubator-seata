package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NotPlaceHolderExprTest {

    @Test
    public void testGet() {
        NotPlaceholderExpr instance = NotPlaceholderExpr.get();
        // Check that the returned instance is not null
        assertEquals(instance, NotPlaceholderExpr.get());
    }

    @Test
    public void testToString() {
        NotPlaceholderExpr instance = NotPlaceholderExpr.get();
        // Check that the toString method returns the expected string
        assertEquals("NOT_PLACEHOLDER", instance.toString());
    }
}