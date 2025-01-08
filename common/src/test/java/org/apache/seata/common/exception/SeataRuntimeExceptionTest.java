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
package org.apache.seata.common.exception;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SeataRuntimeExceptionTest {

    private ErrorCode errorCode;
    private String[] params;

    @BeforeEach
    void setUp() {
        errorCode = ErrorCode.ERR_CONFIG;
        params = new String[] {"param1", "param2"};
    }

    @Test
    void testConstructorWithErrorCodeCauseAndParams() {
        SQLException cause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertNotNull(exception);
        assertEquals(errorCode.getMessage(params), exception.getMessage());
        assertEquals("S0001", exception.getSqlState());
        assertEquals(1000, exception.getVendorCode());
    }

    @Test
    void testToStringShouldReturnLocalizedMessage() {
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, params);
        assertEquals(exception.getLocalizedMessage(), exception.toString());
    }

    @Test
    void testGetVendorCodeWithSQLExceptionCause() {
        SQLException cause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertEquals(1000, exception.getVendorCode());
    }

    @Test
    void testGetSqlStateWithSQLExceptionCause() {
        SQLException cause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertEquals("S0001", exception.getSqlState());
    }

    @Test
    void testGetVendorCodeWithSeataRuntimeExceptionCause() {
        SQLException innerCause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException cause = new SeataRuntimeException(errorCode, innerCause, params);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertEquals(1000, exception.getVendorCode());
    }

    @Test
    void testGetSqlStateWithSeataRuntimeExceptionCause() {
        SQLException innerCause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException cause = new SeataRuntimeException(errorCode, innerCause, params);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertEquals("S0001", exception.getSqlState());
    }
}
