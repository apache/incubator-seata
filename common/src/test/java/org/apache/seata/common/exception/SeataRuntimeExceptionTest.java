package org.apache.seata.common.exception;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SeataRuntimeExceptionTest {

    private ErrorCode errorCode;
    private ErrorCode errorCode1;
    private String[] params;

    @BeforeEach
    void setUp() {
        errorCode = ErrorCode.ERR_CONFIG;
        errorCode1 =  ErrorCode.ERR_CONFIG;
        params = new String[] {"param1", "param2"};
    }

//    @Test
//    void testConstructor_WithErrorCodeAndParams_ShouldSetMessageCorrectly() {
//        SeataRuntimeException exception = new SeataRuntimeException(errorCode1, params);
//        assertNotNull(exception);
//        assertEquals(errorCode1.getMessage(params), exception.getMessage());
//        assertNull(exception.getSqlState());
//        assertEquals(0, exception.getVendorCode());
//    }

    @Test
    void testConstructor_WithErrorCodeCauseAndParams_ShouldSetMessageAndSQLMessageCorrectly() {
        SQLException cause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertNotNull(exception);
        assertEquals(errorCode.getMessage(params), exception.getMessage());
        assertEquals("S0001", exception.getSqlState());
        assertEquals(1000, exception.getVendorCode());
    }

    @Test
    void testToString_ShouldReturnLocalizedMessage() {
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, params);
        assertEquals(exception.getLocalizedMessage(), exception.toString());
    }

//    @Test
//    void testGetMessage_WithCause_ShouldReturnCauseMessage() {
//        SQLException cause = new SQLException("SQL Error");
//        SeataRuntimeException exception = new SeataRuntimeException(errorCode1, cause, params);
//        assertEquals("SQL Error", exception.getMessage());
//    }

//    @Test
//    void testGetMessage_WithoutMessageOrCause_ShouldReturnNull() {
//        SeataRuntimeException exception = new SeataRuntimeException(errorCode1, params);
//        assertNull(exception.getMessage());
//    }

    @Test
    void testGetVendorCode_WithSQLExceptionCause_ShouldReturnVendorCode() {
        SQLException cause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertEquals(1000, exception.getVendorCode());
    }

    @Test
    void testGetSqlState_WithSQLExceptionCause_ShouldReturnSqlState() {
        SQLException cause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertEquals("S0001", exception.getSqlState());
    }

    @Test
    void testGetVendorCode_WithSeataRuntimeExceptionCause_ShouldReturnVendorCode() {
        SQLException innerCause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException cause = new SeataRuntimeException(errorCode, innerCause, params);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertEquals(1000, exception.getVendorCode());
    }

    @Test
    void testGetSqlState_WithSeataRuntimeExceptionCause_ShouldReturnSqlState() {
        SQLException innerCause = new SQLException("SQL Error", "S0001", 1000);
        SeataRuntimeException cause = new SeataRuntimeException(errorCode, innerCause, params);
        SeataRuntimeException exception = new SeataRuntimeException(errorCode, cause, params);
        assertEquals("S0001", exception.getSqlState());
    }
}
