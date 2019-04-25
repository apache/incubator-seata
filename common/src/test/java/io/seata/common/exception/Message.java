/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.common.exception;

import java.sql.SQLException;

/**
 * The type Message.
 *
 * @author Otis.z
 * @since 2019 /3/1
 */
public class Message {

    /**
     * Print.
     */
    public void print() {
        throw FrameworkException.nestedException(new Throwable(Message.class.getSimpleName()));
    }

    /**
     * Print 1.
     */
    public void print1() {
        throw FrameworkException.nestedException("nestedException", new Throwable(Message.class.getSimpleName()));
    }

    /**
     * Print 2.
     *
     * @throws SQLException the sql exception
     */
    public void print2() throws SQLException {
        throw FrameworkException.nestedSQLException("nestedException", new Throwable(Message.class.getSimpleName()));
    }

    /**
     * Print 3.
     *
     * @throws SQLException the sql exception
     */
    public void print3() throws SQLException {
        throw FrameworkException.nestedSQLException(new Throwable(Message.class.getSimpleName()));
    }

    /**
     * Print 4.
     */
    public void print4() {
        throw new FrameworkException();
    }

    /**
     * Print 5.
     */
    public void print5() {
        throw new FrameworkException(FrameworkErrorCode.ExceptionCaught);
    }

    /**
     * Print 6.
     */
    public void print6() {
        throw new FrameworkException("frameworkException", FrameworkErrorCode.InitSeataClientError);
    }

    /**
     * Print 7.
     */
    public void print7() {
        throw new FrameworkException(new Throwable("throw"), "frameworkException",
            FrameworkErrorCode.ChannelIsNotWritable);
    }

    /**
     * Print 8.
     */
    public void print8() {
        throw new FrameworkException(new Throwable("throw"));
    }

    /**
     * Print 9.
     */
    public void print9() {
        throw new FrameworkException(new Throwable(), "frameworkExceptionMsg");
    }

}
