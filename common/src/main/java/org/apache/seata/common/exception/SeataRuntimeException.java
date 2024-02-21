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

public class SeataRuntimeException extends RuntimeException {
    private int vendorCode;
    private String sqlState;
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
            this.sqlState = ((SQLException) e).getSQLState();
        } else if (e instanceof SeataRuntimeException) {
            this.vendorCode = ((SeataRuntimeException) e).getVendorCode();
            this.sqlState = ((SeataRuntimeException) e).getSqlState();
        }
    }

    public int getVendorCode() {
        return vendorCode;
    }

    public String getSqlState() {
        return sqlState;
    }
}
