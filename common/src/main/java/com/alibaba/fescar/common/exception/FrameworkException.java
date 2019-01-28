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
 * The type Framework exception.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /10/9 15:34
 * @FileName: FrameworkException
 * @Description:
 */
public class FrameworkException extends RuntimeException {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkException.class);

    private static final long serialVersionUID = 5531074229174745826L;

    private final FrameworkErrorCode errcode;

    /**
     * Instantiates a new Framework exception.
     */
    public FrameworkException() {
        this(FrameworkErrorCode.UnknownAppError);
    }

    /**
     * Instantiates a new Framework exception.
     *
     * @param err the err
     */
    public FrameworkException(FrameworkErrorCode err) {
        this(err.errMessage, err);
    }

    /**
     * Instantiates a new Framework exception.
     *
     * @param msg the msg
     */
    public FrameworkException(String msg) {
        this(msg, FrameworkErrorCode.UnknownAppError);
    }

    /**
     * Instantiates a new Framework exception.
     *
     * @param msg     the msg
     * @param errCode the err code
     */
    public FrameworkException(String msg, FrameworkErrorCode errCode) {
        this(null, msg, errCode);
    }

    /**
     * Instantiates a new Framework exception.
     *
     * @param cause   the cause
     * @param msg     the msg
     * @param errCode the err code
     */
    public FrameworkException(Throwable cause, String msg, FrameworkErrorCode errCode) {
        super(msg, cause);
        this.errcode = errCode;
    }

    /**
     * Instantiates a new Framework exception.
     *
     * @param th the th
     */
    public FrameworkException(Throwable th) {
        this(th, th.getMessage());
    }

    /**
     * Instantiates a new Framework exception.
     *
     * @param th  the th
     * @param msg the msg
     */
    public FrameworkException(Throwable th, String msg) {
        this(th, msg, FrameworkErrorCode.UnknownAppError);
    }

    /**
     * Gets errcode.
     *
     * @return the errcode
     */
    public FrameworkErrorCode getErrcode() {
        return errcode;
    }

    /**
     * Nested exception framework exception.
     *
     * @param e the e
     * @return the framework exception
     */
    public static FrameworkException nestedException(Throwable e) {
        return nestedException("", e);
    }

    /**
     * Nested exception framework exception.
     *
     * @param msg the msg
     * @param e   the e
     * @return the framework exception
     */
    public static FrameworkException nestedException(String msg, Throwable e) {
        LOGGER.error(msg, e.getMessage(), e);
        if (e instanceof FrameworkException) {
            return (FrameworkException)e;
        }

        return new FrameworkException(e, msg);
    }

    /**
     * Nested sql exception sql exception.
     *
     * @param e the e
     * @return the sql exception
     */
    public static SQLException nestedSQLException(Throwable e) {
        return nestedSQLException("", e);
    }

    /**
     * Nested sql exception sql exception.
     *
     * @param msg the msg
     * @param e   the e
     * @return the sql exception
     */
    public static SQLException nestedSQLException(String msg, Throwable e) {
        LOGGER.error(msg, e.getMessage(), e);
        if (e instanceof SQLException) {
            return (SQLException)e;
        }

        return new SQLException(e);
    }
}
