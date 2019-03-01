package com.alibaba.fescar.common.exception;

import java.sql.SQLException;

/**
 * @author melon.zhao
 * @since 2019/3/1
 */
public class Message {

    public void print() {
        throw FrameworkException.nestedException(new Throwable(Message.class.getSimpleName()));
    }

    public void print1() {
        throw FrameworkException.nestedException("nestedException", new Throwable(Message.class.getSimpleName()));
    }

    public void print2() throws SQLException {
        throw FrameworkException.nestedSQLException("nestedException", new Throwable(Message.class.getSimpleName()));
    }

    public void print3() throws SQLException {
        throw FrameworkException.nestedSQLException(new Throwable(Message.class.getSimpleName()));
    }

    public void print4() {
        throw new FrameworkException();
    }

    public void print5() {
        throw new FrameworkException(FrameworkErrorCode.ExceptionCaught);
    }

    public void print6() {
        throw new FrameworkException("frameworkException", FrameworkErrorCode.InitFescarClientError);
    }

    public void print7() {
        throw new FrameworkException(new Throwable("throw"), "frameworkException",
            FrameworkErrorCode.ChannelIsNotWritable);
    }

    public void print8() {
        throw new FrameworkException(new Throwable("throw"));
    }

    public void print9() {
        throw new FrameworkException(new Throwable(), "frameworkExceptionMsg");
    }



}
