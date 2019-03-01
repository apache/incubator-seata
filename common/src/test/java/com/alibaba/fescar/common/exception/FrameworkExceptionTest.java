/*
 *
 *  *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *
 *
 */

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