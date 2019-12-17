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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dataAccess exception.
 *
 * @author lzf971107
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