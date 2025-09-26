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
 * An interface to be implemented by {@link java.lang.Throwable}
 * extensions which would like to be able to nest root exceptions
 * inside themselves.
 *
 * @since 1.0
 * @version $Id: Nestable.java 512889 2007-02-28 18:18:20Z dlr $
 */
public interface Nestable {

    /**
     * Returns the reference to the exception or error that caused the
     * exception implementing the <code>Nestable</code> to be thrown.
     *
     * @return throwable that caused the original exception
     */
    public Throwable getCause();

    /**
     * Returns the error message of this and any nested
     * <code>Throwable</code>.
     *
     * @return the error message
     */
    public String getMessage();

    /**
     * Returns the error message of the <code>Throwable</code> in the chain
     * of <code>Throwable</code>s at the specified index, numbered from 0.
     *
     * @param index the index of the <code>Throwable</code> in the chain of
     * <code>Throwable</code>s
     * @return the error message, or null if the <code>Throwable</code> at the
     * specified index in the chain does not contain a message
     * @throws IndexOutOfBoundsException if the <code>index</code> argument is
     * negative or not less than the count of <code>Throwable</code>s in the
     * chain
     */
    public String getMessage(int index);

    /**
     * Returns the error message of this and any nested <code>Throwable</code>s
     * in an array of Strings, one element for each message. Any
     * <code>Throwable</code> not containing a message is represented in the
     * array by a null. This has the effect of cause the length of the returned
     * array to be equal to the result of the {@link #getThrowableCount()}
     * operation.
     *
     * @return the error messages
     */
    public String[] getMessages();

    /**
     * Returns the <code>Throwable</code> in the chain of
     * <code>Throwable</code>s at the specified index, numbered from 0.
     *
     * @param index the index, numbered from 0, of the <code>Throwable</code> in
     * the chain of <code>Throwable</code>s
     * @return the <code>Throwable</code>
     * @throws IndexOutOfBoundsException if the <code>index</code> argument is
     * negative or not less than the count of <code>Throwable</code>s in the
     * chain
     */
    public Throwable getThrowable(int index);

    /**
     * Returns the number of nested <code>Throwable</code>s represented by
     * this <code>Nestable</code>, including this <code>Nestable</code>.
     *
     * @return the throwable count
     */
    public int getThrowableCount();

    /**
     * Returns this <code>Nestable</code> and any nested <code>Throwable</code>s
     * in an array of <code>Throwable</code>s, one element for each
     * <code>Throwable</code>.
     *
     * @return the <code>Throwable</code>s
     */
    public Throwable[] getThrowables();

    /**
     * Returns the index, numbered from 0, of the first occurrence of the
     * specified type, or a subclass, in the chain of <code>Throwable</code>s.
     * The method returns -1 if the specified type is not found in the chain.
     * <p>
     * NOTE: From v2.1, we have clarified the <code>Nestable</code> interface
     * such that this method matches subclasses.
     * If you want to NOT match subclasses, please use
     * {@link ExceptionUtils#indexOfThrowable(Throwable, Class)}
     * (which is avaiable in all versions of lang).
     *
     * @param type  the type to find, subclasses match, null returns -1
     * @return index of the first occurrence of the type in the chain, or -1 if
     * the type is not found
     */
    public int indexOfThrowable(Class type);

    /**
     * Returns the index, numbered from 0, of the first <code>Throwable</code>
     * that matches the specified type, or a subclass, in the chain of <code>Throwable</code>s
     * with an index greater than or equal to the specified index.
     * The method returns -1 if the specified type is not found in the chain.
     * <p>
     * NOTE: From v2.1, we have clarified the <code>Nestable</code> interface
     * such that this method matches subclasses.
     * If you want to NOT match subclasses, please use
     * {@link ExceptionUtils#indexOfThrowable(Throwable, Class, int)}
     * (which is avaiable in all versions of lang).
     *
     * @param type  the type to find, subclasses match, null returns -1
     * @param fromIndex the index, numbered from 0, of the starting position in
     * the chain to be searched
     * @return index of the first occurrence of the type in the chain, or -1 if
     * the type is not found
     * @throws IndexOutOfBoundsException if the <code>fromIndex</code> argument
     * is negative or not less than the count of <code>Throwable</code>s in the
     * chain
     */
    public int indexOfThrowable(Class type, int fromIndex);

    /**
     * Prints the stack trace of this exception to the specified print
     * writer.  Includes information from the exception, if any,
     * which caused this exception.
     *
     * @param out <code>PrintWriter</code> to use for output.
     */
    public void printStackTrace(PrintWriter out);

    /**
     * Prints the stack trace of this exception to the specified print
     * stream.  Includes information from the exception, if any,
     * which caused this exception.
     *
     * @param out <code>PrintStream</code> to use for output.
     */
    public void printStackTrace(PrintStream out);

    /**
     * Prints the stack trace for this exception only--root cause not
     * included--using the provided writer.  Used by
     * individual stack traces to a buffer.  The implementation of
     * this method should call
     * <code>super.printStackTrace(out);</code> in most cases.
     *
     * @param out The writer to use.
     */
    public void printPartialStackTrace(PrintWriter out);
}
