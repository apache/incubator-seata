package com.alibaba.fescar.common;


import com.alibaba.fescar.common.exception.FrameworkErrorCode;
import com.alibaba.fescar.common.exception.FrameworkException;
import java.sql.SQLException;
import org.testng.annotations.Test;

/**
 * @author melon.zhao
 * @since 2019/2/28
 */
public class FrameworkExceptionTest {


    @Test(expectedExceptions = FrameworkException.class)
    public void testFrameworkException() {
        throw new FrameworkException(FrameworkErrorCode.ChannelIsNotWritable);
    }


    @Test(expectedExceptions = FrameworkException.class)
    public void testFrameworkException1() {
        throw FrameworkException.nestedException(new Throwable());
    }


    @Test(expectedExceptions = SQLException.class)
    public void testFrameworkException2() throws SQLException {
        throw FrameworkException.nestedSQLException("testFrameworkException", new Throwable());
    }


    @Test(expectedExceptions = SQLException.class)
    public void testFrameworkException3() throws SQLException {
        throw FrameworkException.nestedSQLException(new Throwable());
    }


    @Test(expectedExceptions = FrameworkException.class)
    public void testFrameworkException4() {
        throw FrameworkException.nestedException("testFrameworkException", new Throwable());
    }


}