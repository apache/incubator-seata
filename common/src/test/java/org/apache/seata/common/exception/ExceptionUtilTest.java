package org.apache.seata.common.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author <a href=mailto:ujjboy@qq.com>GengZhang</a>
 */
class ExceptionUtilTest {

    @Test
    public void unwrap() {
        InvocationTargetException targetException = new InvocationTargetException(new RuntimeException("invocation"));
        Assertions.assertInstanceOf(RuntimeException.class, ExceptionUtil.unwrap(targetException));

        UndeclaredThrowableException exception = new UndeclaredThrowableException(new RuntimeException("undeclared"));
        Assertions.assertInstanceOf(RuntimeException.class, ExceptionUtil.unwrap(exception));

        RuntimeException runtimeException = new RuntimeException("runtime");
        Assertions.assertInstanceOf(RuntimeException.class, ExceptionUtil.unwrap(runtimeException));
    }
}