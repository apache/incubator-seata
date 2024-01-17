package io.seata.common.exception;

import java.sql.SQLException;

public class SeataRuntimeException extends RuntimeException {
    private int vendorCode;
    private String SQLState;
    public SeataRuntimeException(ErrorCode errorCode, String... params) {
        super(errorCode.getMessage(params));
        this.vendorCode = errorCode.getCode();
    }

    public SeataRuntimeException(ErrorCode errorCode, Throwable cause, String... params) {
        super(errorCode.getMessage(params), cause);
        buildSQLMessage(cause);
    }

    @Override
    public String toString() {
        return super.getLocalizedMessage();
    }

    @Override
    public String getMessage() {
        if (super.getMessage() != null) {
            return super.getMessage();
        } else if (getCause() != null) {
            Throwable ca = getCause();
            if (ca != null) {
                return ca.getMessage();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void buildSQLMessage(Throwable e) {
        if (e instanceof SQLException) {
            this.vendorCode = ((SQLException) e).getErrorCode();
            this.SQLState = ((SQLException) e).getSQLState();
        } else if (e instanceof SeataRuntimeException) {
            this.vendorCode = ((SeataRuntimeException) e).getVendorCode();
            this.SQLState = ((SeataRuntimeException) e).getSQLState();
        }
    }

    public int getVendorCode() {
        return vendorCode;
    }

    public String getSQLState() {
        return SQLState;
    }
}
