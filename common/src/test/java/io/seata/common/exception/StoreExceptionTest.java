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

public class StoreExceptionTest {

    @Test
    public void testConstructorWithNoParameters() {
        try {
            throw new StoreException();
        } catch (StoreException exception) {
            exceptionAsserts(exception);
        }
    }

    @Test
    public void testConstructorWithFrameworkErrorCode() {
        try {
            throw new StoreException(FrameworkErrorCode.UnknownAppError);
        } catch (StoreException exception) {
            exceptionAsserts(exception);
        }
    }

    @Test
    public void testConstructorWithMessage() {
        try {
            throw new StoreException(FrameworkErrorCode.UnknownAppError.getErrMessage());
        } catch (StoreException exception) {
            exceptionAsserts(exception);
        }
    }

    @Test
    public void testConstructorWithMessageAndFrameworkErrorCode() {
        try {
            throw new StoreException(
                    FrameworkErrorCode.UnknownAppError.getErrMessage(),
                    FrameworkErrorCode.UnknownAppError);
        } catch (StoreException exception) {
            exceptionAsserts(exception);
        }
    }

    @Test
    public void testConstructorWithCauseExceptionMessageAndFrameworkErrorCode() {
        try {
            throw new StoreException(
                    new Throwable(),
                    FrameworkErrorCode.UnknownAppError.getErrMessage(),
                    FrameworkErrorCode.UnknownAppError);
        } catch (StoreException exception) {
            exceptionAsserts(exception);
        }
    }

    @Test
    public void testConstructorWithThrowable() {
        try {
            throw new StoreException(new Throwable(FrameworkErrorCode.UnknownAppError.getErrMessage()));
        } catch (StoreException exception) {
            exceptionAsserts(exception);
        }
    }

    @Test
    public void testConstructorWithThrowableAndMessage() {
        try {
            throw new StoreException(new Throwable(), FrameworkErrorCode.UnknownAppError.getErrMessage());
        } catch (StoreException exception) {
            exceptionAsserts(exception);
        }
    }

    private static void exceptionAsserts(StoreException exception) {
        assertThat(exception)
                .isInstanceOf(StoreException.class)
                .hasMessage(FrameworkErrorCode.UnknownAppError.getErrMessage());
        assertThat(exception.getErrcode())
                .isEqualTo(FrameworkErrorCode.UnknownAppError);
    }
}
