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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Framework exception test.
 *
 * @author Otis.z
 */
public class FrameworkExceptionTest {

    private Message message = new Message();

    /**
     * Test get errcode.
     */
    @Test
    public void testGetErrcode() {
        Throwable throwable = Assertions.assertThrows(FrameworkException.class, () -> {
            message.print4();
        });
        assertThat(throwable).hasMessage(FrameworkErrorCode.UnknownAppError.getErrMessage());
        assertThat(((FrameworkException)throwable).getErrcode()).isEqualTo(FrameworkErrorCode.UnknownAppError);
    }

    /**
     * Test nested exception.
     */
    @Test
    public void testNestedException() {
        Throwable throwable = Assertions.assertThrows(FrameworkException.class, () -> {
            message.print();
        });
        assertThat(throwable).hasMessage("");
    }

    /**
     * Test nested exception 1.
     */
    @Test
    public void testNestedException1() {
        Throwable throwable = Assertions.assertThrows(FrameworkException.class, () -> {
            message.print1();
        });
        assertThat(throwable).hasMessage("nestedException");
    }

    /**
     * Test nested exception 2.
     */
    @Test
    public void testNestedException2() {
        Throwable throwable = Assertions.assertThrows(SQLException.class, () -> {
            message.print2();
        });
        assertThat(throwable).hasMessageContaining("Message");
    }

    /**
     * Test nested exception 3.
     */
    @Test
    public void testNestedException3() {
        Throwable throwable = Assertions.assertThrows(SQLException.class, () -> {
            message.print3();
        });
        assertThat(throwable).hasMessageContaining("Message");
    }

    /**
     * Test nested exception 5.
     */
    @Test
    public void testNestedException5() {
        Throwable throwable = Assertions.assertThrows(FrameworkException.class, () -> {
            message.print5();
        });
        assertThat(throwable).hasMessage(FrameworkErrorCode.ExceptionCaught.getErrMessage());
    }

    /**
     * Test nested exception 6.
     */
    @Test
    public void testNestedException6() {
        Throwable throwable = Assertions.assertThrows(FrameworkException.class, () -> {
            message.print6();
        });
        assertThat(throwable).hasMessage("frameworkException");
    }

    /**
     * Test nested exception 7.
     */
    @Test
    public void testNestedException7() {
        Throwable throwable = Assertions.assertThrows(FrameworkException.class, () -> {
            message.print7();
        });
        assertThat(throwable).hasMessage("frameworkException");
    }

    /**
     * Test nested exception 8.
     */
    @Test
    public void testNestedException8() {
        Throwable throwable = Assertions.assertThrows(FrameworkException.class, () -> {
            message.print8();
        });
        assertThat(throwable).hasMessage("throw");
    }

    /**
     * Test nested exception 9.
     */
    @Test
    public void testNestedException9() {
        Throwable throwable = Assertions.assertThrows(FrameworkException.class, () -> {
            message.print9();
        });
        assertThat(throwable).hasMessage("frameworkExceptionMsg");
    }

    private static void exceptionAsserts(FrameworkException exception, String expectMessage) {
        if (null == expectMessage) {
            expectMessage = FrameworkErrorCode.UnknownAppError.getErrMessage();
        }
        assertThat(exception).isInstanceOf(FrameworkException.class).hasMessage(expectMessage);
        assertThat(exception.getErrcode()).isEqualTo(FrameworkErrorCode.UnknownAppError);
    }

}
