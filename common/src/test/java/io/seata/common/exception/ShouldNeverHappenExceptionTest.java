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
 * The shouldNeverHappen exception.
 *
 * @author lzf971107
 */
public class ShouldNeverHappenExceptionTest {

    @Test
    public void testConstructorWithNoParameters() {
        assertThat(new ShouldNeverHappenException()).isInstanceOf(ShouldNeverHappenException.class);
    }

    @Test
    public void testConstructorWithMessage() {
        exceptionAsserts(new ShouldNeverHappenException(FrameworkErrorCode.UnknownAppError.getErrMessage()));
    }

    @Test
    public void testConstructorWithMessageAndThrowable() {
        exceptionAsserts(new ShouldNeverHappenException(FrameworkErrorCode.UnknownAppError.getErrMessage(), new Throwable()));
    }

    @Test
    public void testConstructorWithThrowable() {
        assertThat(new ShouldNeverHappenException(new Throwable(FrameworkErrorCode.UnknownAppError.getErrMessage()))).isInstanceOf(ShouldNeverHappenException.class).hasMessage("java.lang.Throwable: " + FrameworkErrorCode.UnknownAppError.getErrMessage());
    }

    private static void exceptionAsserts(ShouldNeverHappenException exception) {
        assertThat(exception).isInstanceOf(ShouldNeverHappenException.class).hasMessage(FrameworkErrorCode.UnknownAppError.getErrMessage());
    }
}