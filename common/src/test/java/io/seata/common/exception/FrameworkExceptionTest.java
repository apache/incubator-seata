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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Framework exception test.
 *
 * @author Otis.z
 * @date 2019 /3/1
 */
public class FrameworkExceptionTest {

    private Message message = new Message();

    /**
     * Test get errcode.
     */
    @Test
    public void testGetErrcode() {
        try {
            message.print4();
        } catch (FrameworkException e) {
            assertThat(e).isInstanceOf(FrameworkException.class).hasMessage(
                    FrameworkErrorCode.UnknownAppError.getErrMessage());
            assertThat(e.getErrcode()).isEqualTo(FrameworkErrorCode.UnknownAppError);
        }
    }

    /**
     * Test nested exception.
     */
    @Test
    public void testNestedException() {
        try {
            message.print();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(FrameworkException.class).hasMessage("");
        }
    }

    /**
     * Test nested exception 1.
     */
    @Test
    public void testNestedException1() {
        try {
            message.print1();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(FrameworkException.class).hasMessage("nestedException");
        }
    }

    /**
     * Test nested exception 2.
     */
    @Test
    public void testNestedException2() {
        try {
            message.print2();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(SQLException.class).hasMessageContaining("Message");
        }
    }

    /**
     * Test nested exception 3.
     */
    @Test
    public void testNestedException3() {
        try {
            message.print3();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(SQLException.class).hasMessageContaining("Message");
        }
    }

    /**
     * Test nested exception 5.
     */
    @Test
    public void testNestedException5() {
        try {
            message.print5();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(FrameworkException.class).hasMessage(
                    FrameworkErrorCode.ExceptionCaught.getErrMessage());
        }
    }

    /**
     * Test nested exception 6.
     */
    @Test
    public void testNestedException6() {
        try {
            message.print6();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(FrameworkException.class).hasMessage("frameworkException");
        }
    }

    /**
     * Test nested exception 7.
     */
    @Test
    public void testNestedException7() {
        try {
            message.print7();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(FrameworkException.class).hasMessage("frameworkException");
        }
    }

    /**
     * Test nested exception 8.
     */
    @Test
    public void testNestedException8() {
        try {
            message.print8();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(FrameworkException.class).hasMessage("throw");
        }
    }

    /**
     * Test nested exception 9.
     */
    @Test
    public void testNestedException9() {
        try {
            message.print9();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(FrameworkException.class).hasMessage("frameworkExceptionMsg");
        }
    }

}
