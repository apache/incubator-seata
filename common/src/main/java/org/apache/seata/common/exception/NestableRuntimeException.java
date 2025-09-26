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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The base class of all runtime exceptions which can contain other
 * exceptions.
 *
 * @since 1.0
 * @version $Id: NestableRuntimeException.java 512889 2007-02-28 18:18:20Z dlr $
 */
public class NestableRuntimeException extends RuntimeException implements Nestable {

    /**
     * Required for serialization support.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 1L;

    /**
     * The helper instance which contains much of the code which we
     * delegate to.
     */
    protected NestableDelegate delegate = new NestableDelegate(this);

    /**
     * Holds the reference to the exception or error that caused
     * this exception to be thrown.
     */
    private Throwable cause = null;

    /**
     * Constructs a new <code>NestableRuntimeException</code> without specified
     * detail message.
     */
    public NestableRuntimeException() {
        super();
    }

    /**
     * Constructs a new <code>NestableRuntimeException</code> with specified
     * detail message.
     *
     * @param msg the error message
     */
    public NestableRuntimeException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new <code>NestableRuntimeException</code> with specified
     * nested <code>Throwable</code>.
     *
     * @param cause the exception or error that caused this exception to be
     * thrown
     */
    public NestableRuntimeException(Throwable cause) {
        super();
        this.cause = cause;
    }

    /**
     * Constructs a new <code>NestableRuntimeException</code> with specified
     * detail message and nested <code>Throwable</code>.
     *
     * @param msg    the error message
     * @param cause  the exception or error that caused this exception to be
     * thrown
     */
    public NestableRuntimeException(String msg, Throwable cause) {
        super(msg);
        this.cause = cause;
    }

    /**
     * {@inheritDoc}
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns the detail message string of this throwable. If it was
     * created with a null message, returns the following:
     * (cause==null ? null : cause.toString()).
     *
     * @return String message string of the throwable
     */
    public String getMessage() {
        if (super.getMessage() != null) {
            return super.getMessage();
        } else if (cause != null) {
            return cause.toString();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getMessage(int index) {
        if (index == 0) {
            return super.getMessage();
        }
        return delegate.getMessage(index);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getMessages() {
        return delegate.getMessages();
    }

    /**
     * {@inheritDoc}
     */
    public Throwable getThrowable(int index) {
        return delegate.getThrowable(index);
    }

    /**
     * {@inheritDoc}
     */
    public int getThrowableCount() {
        return delegate.getThrowableCount();
    }

    /**
     * {@inheritDoc}
     */
    public Throwable[] getThrowables() {
        return delegate.getThrowables();
    }

    /**
     * {@inheritDoc}
     */
    public int indexOfThrowable(Class type) {
        return delegate.indexOfThrowable(type, 0);
    }

    /**
     * {@inheritDoc}
     */
    public int indexOfThrowable(Class type, int fromIndex) {
        return delegate.indexOfThrowable(type, fromIndex);
    }

    /**
     * {@inheritDoc}
     */
    public void printStackTrace() {
        delegate.printStackTrace();
    }

    /**
     * {@inheritDoc}
     */
    public void printStackTrace(PrintStream out) {
        delegate.printStackTrace(out);
    }

    /**
     * {@inheritDoc}
     */
    public void printStackTrace(PrintWriter out) {
        delegate.printStackTrace(out);
    }

    /**
     * {@inheritDoc}
     */
    public final void printPartialStackTrace(PrintWriter out) {
        super.printStackTrace(out);
    }
}
