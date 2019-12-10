package io.seata.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dataAccess exception.
 *
 * @author withthewind
 */
public class DataAccessExceptionTest {

    @Test
    public void testConstructorWithNoParameters() {
        exceptionAsserts(new DataAccessException());
    }

    @Test
    public void testConstructorWithFrameworkErrorCode() {
        exceptionAsserts(new DataAccessException(FrameworkErrorCode.UnknownAppError));
    }

    @Test
    public void testConstructorWithMessage() {
        exceptionAsserts(new DataAccessException(FrameworkErrorCode.UnknownAppError.getErrMessage()));
    }

    @Test
    public void testConstructorWithMessageAndFrameworkErrorCode() {
        exceptionAsserts(new DataAccessException(FrameworkErrorCode.UnknownAppError.getErrMessage(), FrameworkErrorCode.UnknownAppError));
    }

    @Test
    public void testConstructorWithCauseExceptionMessageAndFrameworkErrorCode() {
        exceptionAsserts(new DataAccessException(new Throwable(), FrameworkErrorCode.UnknownAppError.getErrMessage(), FrameworkErrorCode.UnknownAppError));
    }

    @Test
    public void testConstructorWithThrowable() {
        exceptionAsserts(new DataAccessException(new Throwable(FrameworkErrorCode.UnknownAppError.getErrMessage())));
    }

    private static void exceptionAsserts(DataAccessException exception) {
        assertThat(exception).isInstanceOf(DataAccessException.class).hasMessage(FrameworkErrorCode.UnknownAppError.getErrMessage());
        assertThat(exception.getErrcode()).isEqualTo(FrameworkErrorCode.UnknownAppError);
    }
}