package org.apache.seata.sqlparser;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class SQLParsingExceptionTest {
    @Test
    public void testConstructorWithMessage() {
        String message = "Test message";
        SQLParsingException exception = new SQLParsingException(message);
        assertEquals(message, exception.getMessage(), "Message should match");
        assertNull(exception.getCause(), "Cause should be null");
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Test message";
        Throwable cause = new IllegalArgumentException("Test cause");
        SQLParsingException exception = new SQLParsingException(message, cause);
        assertEquals(message, exception.getMessage(), "Message should match");
        assertEquals(cause, exception.getCause(), "Cause should match");
    }

    @Test
    public void testConstructorWithCause() {
        Throwable cause = new IllegalArgumentException("Test cause");
        SQLParsingException exception = new SQLParsingException(cause);
        assertEquals(cause.toString(), exception.getMessage(), "Message should be cause's toString");
        assertEquals(cause, exception.getCause(), "Cause should match");
    }
}
