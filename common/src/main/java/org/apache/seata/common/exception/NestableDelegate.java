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

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>A shared implementation of the nestable exception functionality.</p>
 *
 * @since 1.0
 * @version $Id: NestableDelegate.java 905636 2010-02-02 14:03:32Z niallp $
 */
public class NestableDelegate implements Serializable {

    /**
     * Required for serialization support.
     *
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor error message.
     */
    private static final transient String MUST_BE_THROWABLE =
            "The Nestable implementation passed to the NestableDelegate(Nestable) "
                    + "constructor must extend java.lang.Throwable";

    /**
     * Holds the reference to the exception or error that we're
     * wrapping
     */
    private Throwable nestable = null;

    /**
     * Whether to print the stack trace top-down.
     * This public flag may be set by calling code, typically in initialisation.
     * This exists for backwards compatability, setting it to false will return
     * the library to v1.0 behaviour (but will affect all users of the library
     * in the classloader).
     * @since 2.0
     */
    public static boolean topDown = true;

    /**
     * Whether to trim the repeated stack trace.
     * This public flag may be set by calling code, typically in initialisation.
     * This exists for backwards compatability, setting it to false will return
     * the library to v1.0 behaviour (but will affect all users of the library
     * in the classloader).
     * @since 2.0
     */
    public static boolean trimStackFrames = true;

    /**
     * Whether to match subclasses via indexOf.
     * This public flag may be set by calling code, typically in initialisation.
     * This exists for backwards compatability, setting it to false will return
     * the library to v2.0 behaviour (but will affect all users of the library
     * in the classloader).
     * @since 2.1
     */
    public static boolean matchSubclasses = true;

    /**
     * Constructs a new <code>NestableDelegate</code> instance to manage the
     * specified <code>Nestable</code>.
     *
     * @param nestable the Nestable implementation (<i>must</i> extend
     * {@link java.lang.Throwable})
     * @since 2.0
     */
    public NestableDelegate(Nestable nestable) {
        if (nestable instanceof Throwable) {
            this.nestable = (Throwable) nestable;
        } else {
            throw new IllegalArgumentException(MUST_BE_THROWABLE);
        }
    }

    /**
     * Returns the error message of the <code>Throwable</code> in the chain of <code>Throwable</code>s at the
     * specified index, numbered from 0.
     *
     * @param index
     *            the index of the <code>Throwable</code> in the chain of <code>Throwable</code>s
     * @return the error message, or null if the <code>Throwable</code> at the specified index in the chain does not
     *         contain a message
     * @throws IndexOutOfBoundsException
     *             if the <code>index</code> argument is negative or not less than the count of <code>Throwable</code>s
     *             in the chain
     * @since 2.0
     */
    public String getMessage(int index) {
        Throwable t = this.getThrowable(index);
        if (Nestable.class.isInstance(t)) {
            return ((Nestable) t).getMessage(0);
        }
        return t.getMessage();
    }

    /**
     * Returns the full message contained by the <code>Nestable</code> and any nested <code>Throwable</code>s.
     *
     * @param baseMsg
     *            the base message to use when creating the full message. Should be generally be called via
     *            <code>nestableHelper.getMessage(super.getMessage())</code>, where <code>super</code> is an
     *            instance of {@link java.lang.Throwable}.
     * @return The concatenated message for this and all nested <code>Throwable</code>s
     * @since 2.0
     */
    public String getMessage(String baseMsg) {
        Throwable nestedCause = ExceptionUtils.getCause(this.nestable);
        String causeMsg = nestedCause == null ? null : nestedCause.getMessage();
        if (nestedCause == null || causeMsg == null) {
            return baseMsg; // may be null, which is a valid result
        }
        if (baseMsg == null) {
            return causeMsg;
        }
        return baseMsg + ": " + causeMsg;
    }

    /**
     * Returns the error message of this and any nested <code>Throwable</code>s in an array of Strings, one element
     * for each message. Any <code>Throwable</code> not containing a message is represented in the array by a null.
     * This has the effect of cause the length of the returned array to be equal to the result of the
     * {@link #getThrowableCount()} operation.
     *
     * @return the error messages
     * @since 2.0
     */
    public String[] getMessages() {
        Throwable[] throwables = this.getThrowables();
        String[] msgs = new String[throwables.length];
        for (int i = 0; i < throwables.length; i++) {
            msgs[i] = (Nestable.class.isInstance(throwables[i])
                    ? ((Nestable) throwables[i]).getMessage(0)
                    : throwables[i].getMessage());
        }
        return msgs;
    }

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
     * @since 2.0
     */
    public Throwable getThrowable(int index) {
        if (index == 0) {
            return this.nestable;
        }
        Throwable[] throwables = this.getThrowables();
        return throwables[index];
    }

    /**
     * Returns the number of <code>Throwable</code>s contained in the
     * <code>Nestable</code> contained by this delegate.
     *
     * @return the throwable count
     * @since 2.0
     */
    public int getThrowableCount() {
        return ExceptionUtils.getThrowableCount(this.nestable);
    }

    /**
     * Returns this delegate's <code>Nestable</code> and any nested
     * <code>Throwable</code>s in an array of <code>Throwable</code>s, one
     * element for each <code>Throwable</code>.
     *
     * @return the <code>Throwable</code>s
     * @since 2.0
     */
    public Throwable[] getThrowables() {
        return ExceptionUtils.getThrowables(this.nestable);
    }

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
     * An alternative is to use the public static flag {@link #matchSubclasses}
     * on <code>NestableDelegate</code>, however this is not recommended.
     *
     * @param type  the type to find, subclasses match, null returns -1
     * @param fromIndex the index, numbered from 0, of the starting position in
     * the chain to be searched
     * @return index of the first occurrence of the type in the chain, or -1 if
     * the type is not found
     * @throws IndexOutOfBoundsException if the <code>fromIndex</code> argument
     * is negative or not less than the count of <code>Throwable</code>s in the
     * chain
     * @since 2.0
     */
    public int indexOfThrowable(Class type, int fromIndex) {
        if (type == null) {
            return -1;
        }
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("The start index was out of bounds: " + fromIndex);
        }
        Throwable[] throwables = ExceptionUtils.getThrowables(this.nestable);
        if (fromIndex >= throwables.length) {
            throw new IndexOutOfBoundsException(
                    "The start index was out of bounds: " + fromIndex + " >= " + throwables.length);
        }
        if (matchSubclasses) {
            for (int i = fromIndex; i < throwables.length; i++) {
                if (type.isAssignableFrom(throwables[i].getClass())) {
                    return i;
                }
            }
        } else {
            for (int i = fromIndex; i < throwables.length; i++) {
                if (type.equals(throwables[i].getClass())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Prints the stack trace of this exception the the standar error
     * stream.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack trace of this exception to the specified
     * stream.
     *
     * @param out <code>PrintStream</code> to use for output.
     * @see #printStackTrace(PrintWriter)
     */
    public void printStackTrace(PrintStream out) {
        synchronized (out) {
            PrintWriter pw = new PrintWriter(out, false);
            printStackTrace(pw);
            // Flush the PrintWriter before it's GC'ed.
            pw.flush();
        }
    }

    /**
     * Prints the stack trace of this exception to the specified
     * writer. If the Throwable class has a <code>getCause</code>
     * method (i.e. running on jre1.4 or higher), this method just
     * uses Throwable's printStackTrace() method. Otherwise, generates
     * the stack-trace, by taking into account the 'topDown' and
     * 'trimStackFrames' parameters. The topDown and trimStackFrames
     * are set to 'true' by default (produces jre1.4-like stack trace).
     *
     * @param out <code>PrintWriter</code> to use for output.
     */
    public void printStackTrace(PrintWriter out) {
        if (this.nestable != null) {
            this.nestable.printStackTrace(out);
        }
    }

    /**
     * Captures the stack trace associated with the specified
     * <code>Throwable</code> object, decomposing it into a list of
     * stack frames.
     *
     * @param t The <code>Throwable</code>.
     * @return  An array of strings describing each stack frame.
     * @since 2.0
     */
    protected String[] getStackFrames(Throwable t) {
        return ExceptionUtils.getStackFrames(t);
    }

    /**
     * Trims the stack frames. The first set is left untouched. The rest
     * of the frames are truncated from the bottom by comparing with
     * one just on top.
     *
     * @param stacks The list containing String[] elements
     * @since 2.0
     */
    protected void trimStackFrames(List stacks) {
        for (int size = stacks.size(), i = size - 1; i > 0; i--) {
            String[] curr = (String[]) stacks.get(i);
            String[] next = (String[]) stacks.get(i - 1);

            List currList = new ArrayList(Arrays.asList(curr));
            List nextList = new ArrayList(Arrays.asList(next));
            ExceptionUtils.removeCommonFrames(currList, nextList);

            int trimmed = curr.length - currList.size();
            if (trimmed > 0) {
                currList.add("\t... " + trimmed + " more");
                stacks.set(i, currList.toArray(new String[currList.size()]));
            }
        }
    }
}
