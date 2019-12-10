package io.seata.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The eurekaRegistry exception.
 *
 * @author withthewind
 */
public class EurekaRegistryExceptionTest {

    @Test
    public void testConstructorWithNoParameters() {
        assertThat(new EurekaRegistryException()).isInstanceOf(EurekaRegistryException.class);
    }

    @Test
    public void testConstructorWithMessage() {
        exceptionAsserts(new EurekaRegistryException(FrameworkErrorCode.UnknownAppError.getErrMessage()));
    }

    @Test
    public void testConstructorWithMessageAndThrowable() {
        exceptionAsserts(new EurekaRegistryException(FrameworkErrorCode.UnknownAppError.getErrMessage(), new Throwable()));
    }

    @Test
    public void testConstructorWithThrowable() {
        exceptionAsserts(new EurekaRegistryException(new Throwable(FrameworkErrorCode.UnknownAppError.getErrMessage())));
    }

    private static void exceptionAsserts(EurekaRegistryException exception) {
        assertThat(exception).isInstanceOf(EurekaRegistryException.class).hasMessage(FrameworkErrorCode.UnknownAppError.getErrMessage());
    }
}