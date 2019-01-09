/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.common.exception;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/10/9 15:34
 * @FileName: FrameworkException
 * @Description:
 */
public class FrameworkException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkException.class);

    private static final long serialVersionUID = 5531074229174745826L;

    private final FrameworkErrorCode errcode;

    public FrameworkException() {
        this(FrameworkErrorCode.UnknownAppError);
    }

    public FrameworkException(FrameworkErrorCode err) {
        this(err.errMessage, err);
    }

    public FrameworkException(String msg) {
        this(msg, FrameworkErrorCode.UnknownAppError);
    }

    public FrameworkException(String msg, FrameworkErrorCode errCode) {
        this(null, msg, errCode);
    }

    public FrameworkException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(msg, cause);
        this.errcode = errCode;
    }

    public FrameworkException(Throwable th) {
        this(th, th.getMessage());
    }

    public FrameworkException(Throwable th, String msg) {
        this(th, msg, FrameworkErrorCode.UnknownAppError);
    }

    public FrameworkErrorCode getErrcode() {
        return errcode;
    }

    public static FrameworkException nestedException(Throwable e) {
        return nestedException("", e);
    }

    public static FrameworkException nestedException(String msg, Throwable e) {
        LOGGER.error(msg, e.getMessage(), e);
        if (e instanceof FrameworkException) {
            return (FrameworkException)e;
        }

        return new FrameworkException(e, msg);
    }

    public static SQLException nestedSQLException(Throwable e) {
        return nestedSQLException("", e);
    }

    public static SQLException nestedSQLException(String msg, Throwable e) {
        LOGGER.error(msg, e.getMessage(), e);
        if (e instanceof SQLException) {
            return (SQLException)e;
        }

        return new SQLException(e);
    }
}
