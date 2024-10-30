package org.apache.seata.common.exception;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExceptionUtilTest {
    private Exception exception;

    @BeforeEach
    public void setUp() {
        exception = new Exception();
    }

    @Test
    public void unwrap_InvocationTargetException_ReturnsCause() {
        InvocationTargetException ite = new InvocationTargetException(exception, "test");
        Throwable result = ExceptionUtil.unwrap(ite);
        Assertions.assertSame(exception, result, "Expected the unwrapped exception to be the cause of InvocationTargetException.");
    }

    @Test
    public void unwrap_UndeclaredThrowableException_ReturnsCause() {
        UndeclaredThrowableException ute = new UndeclaredThrowableException(exception, "test");
        Throwable result = ExceptionUtil.unwrap(ute);
        Assertions.assertSame(exception, result, "Expected the unwrapped exception to be the cause of UndeclaredThrowableException.");
    }

    @Test
    public void unwrap_NestedInvocationTargetException_ReturnsRootCause() {
        Exception rootCause = new Exception();
        InvocationTargetException ite = new InvocationTargetException(new UndeclaredThrowableException(rootCause, "test"), "test");
        Throwable result = ExceptionUtil.unwrap(ite);
        Assertions.assertSame(rootCause, result, "Expected the unwrapped exception to be the root cause.");
    }

    @Test
    public void unwrap_NotWrappedException_ReturnsSameException() {
        Throwable result = ExceptionUtil.unwrap(exception);
        Assertions.assertSame(exception, result, "Expected the unwrapped exception to be the same as the input when no wrapping is present.");
    }
}
