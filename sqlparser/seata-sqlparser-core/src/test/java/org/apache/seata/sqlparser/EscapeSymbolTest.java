package org.apache.seata.sqlparser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EscapeSymbolTest {

    @Test
    public void testGetLeftSymbol() {
        char expectedLeftSymbol = '"';
        EscapeSymbol escapeSymbol = new EscapeSymbol(expectedLeftSymbol, '"');
        assertEquals(expectedLeftSymbol, escapeSymbol.getLeftSymbol(),
                "The left symbol should be '" + expectedLeftSymbol + "'");
    }

    @Test
    public void testGetRightSymbol() {
        char expectedRightSymbol = '"';
        EscapeSymbol escapeSymbol = new EscapeSymbol('"', expectedRightSymbol);
        assertEquals(expectedRightSymbol, escapeSymbol.getRightSymbol(),
                "The right symbol should be '" + expectedRightSymbol + "'");
    }
}
