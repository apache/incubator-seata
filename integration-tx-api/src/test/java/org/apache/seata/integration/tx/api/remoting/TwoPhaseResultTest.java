package org.apache.seata.integration.tx.api.remoting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TwoPhaseResultTest {

    private TwoPhaseResult result;

    @BeforeEach
    public void setUp() {
        result = new TwoPhaseResult(false, "");
    }

    @Test
    public void testGetMessage() {
        result.setMessage("message");
        assertEquals("message", result.getMessage());
    }

    @Test
    public void testIsSuccess() {
        result.setSuccess(true);
        assertTrue(result.isSuccess());
    }

    @Test
    public void testToStringEmptyMessage() {
        assertEquals("[isSuccess:false]", result.toString());
    }

    @Test
    public void testToStringNotEmptyMessage() {
        result.setMessage("test");
        assertEquals("[isSuccess:false, msg:test]", result.toString());
    }
}
