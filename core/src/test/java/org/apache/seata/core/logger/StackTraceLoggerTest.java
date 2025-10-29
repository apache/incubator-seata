/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.logger;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StackTraceLoggerTest {

    @Test
    public void testInfoWithEnabledLogger() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        Throwable cause = new RuntimeException("test exception");
        String format = "Test message: {}";
        Object[] args = new Object[] {"arg1"};

        StackTraceLogger.info(mockLogger, cause, format, args);

        verify(mockLogger, times(1)).isInfoEnabled();
        verify(mockLogger, times(1)).info(anyString(), any(Object[].class));
    }

    @Test
    public void testInfoWithDisabledLogger() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isInfoEnabled()).thenReturn(false);

        Throwable cause = new RuntimeException("test exception");
        String format = "Test message: {}";
        Object[] args = new Object[] {"arg1"};

        StackTraceLogger.info(mockLogger, cause, format, args);

        verify(mockLogger, times(1)).isInfoEnabled();
        verify(mockLogger, never()).info(anyString(), any(Object[].class));
    }

    @Test
    public void testInfoWithNullArgs() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        Throwable cause = new RuntimeException("test exception");
        String format = "Test message";
        Object[] args = null;

        StackTraceLogger.info(mockLogger, cause, format, args);

        verify(mockLogger, times(1)).isInfoEnabled();
        verify(mockLogger, times(1)).info(anyString(), (Object[]) any());
    }

    @Test
    public void testWarnWithEnabledLogger() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isWarnEnabled()).thenReturn(true);

        Throwable cause = new RuntimeException("test exception");
        String format = "Test warning: {}";
        Object[] args = new Object[] {"arg1"};

        StackTraceLogger.warn(mockLogger, cause, format, args);

        verify(mockLogger, times(1)).isWarnEnabled();
        verify(mockLogger, times(1)).warn(anyString(), any(Object[].class));
    }

    @Test
    public void testWarnWithDisabledLogger() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isWarnEnabled()).thenReturn(false);

        Throwable cause = new RuntimeException("test exception");
        String format = "Test warning: {}";
        Object[] args = new Object[] {"arg1"};

        StackTraceLogger.warn(mockLogger, cause, format, args);

        verify(mockLogger, times(1)).isWarnEnabled();
        verify(mockLogger, never()).warn(anyString(), any(Object[].class));
    }

    @Test
    public void testErrorWithEnabledLogger() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isErrorEnabled()).thenReturn(true);

        Throwable cause = new RuntimeException("test exception");
        String format = "Test error: {}";
        Object[] args = new Object[] {"arg1"};

        StackTraceLogger.error(mockLogger, cause, format, args);

        verify(mockLogger, times(1)).isErrorEnabled();
        verify(mockLogger, times(1)).error(anyString(), any(Object[].class));
    }

    @Test
    public void testErrorWithDisabledLogger() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isErrorEnabled()).thenReturn(false);

        Throwable cause = new RuntimeException("test exception");
        String format = "Test error: {}";
        Object[] args = new Object[] {"arg1"};

        StackTraceLogger.error(mockLogger, cause, format, args);

        verify(mockLogger, times(1)).isErrorEnabled();
        verify(mockLogger, never()).error(anyString(), any(Object[].class));
    }

    @Test
    public void testErrorWithEmptyArgs() {
        Logger mockLogger = mock(Logger.class);
        when(mockLogger.isErrorEnabled()).thenReturn(true);

        Throwable cause = new RuntimeException("test exception");
        String format = "Test error";
        Object[] args = new Object[] {};

        StackTraceLogger.error(mockLogger, cause, format, args);

        verify(mockLogger, times(1)).isErrorEnabled();
        verify(mockLogger, times(1)).error(anyString(), any(Object[].class));
    }
}
