package com.alibaba.fescar.common.exception;

import java.sql.SQLException;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * @author melon.zhao
 * @since 2019/3/1
 */
public class FrameworkExceptionTest {

    Message message = new Message();

    @Test
    public void testGetErrcode() {
        try {
            message.print4();
        } catch (FrameworkException e) {
            Assert.assertEquals(e.getErrcode(), FrameworkErrorCode.UnknownAppError);
        }
    }

    @Test(expectedExceptions = FrameworkException.class)
    public void testNestedException() {
        message.print();
    }

    @Test(expectedExceptions = FrameworkException.class)
    public void testNestedException1() {
        message.print1();
    }

    @Test(expectedExceptions = FrameworkException.class)
    public void testNestedException2() {
        message.print1();
    }

    @Test(expectedExceptions = SQLException.class)
    public void testNestedException3() throws SQLException {
        message.print2();
    }

    @Test(expectedExceptions = SQLException.class)
    public void testNestedException4() throws SQLException {
        message.print3();
    }

    @Test(expectedExceptions = FrameworkException.class)
    public void testNestedException5() {
        message.print5();
    }

    @Test(expectedExceptions = FrameworkException.class)
    public void testNestedException6() {
        message.print6();
    }

    @Test(expectedExceptions = FrameworkException.class)
    public void testNestedException7() {
        message.print7();
    }

    @Test(expectedExceptions = FrameworkException.class)
    public void testNestedException8() {
        message.print8();
    }

    @Test(expectedExceptions = FrameworkException.class)
    public void testNestedException9() {
        message.print9();
    }


}