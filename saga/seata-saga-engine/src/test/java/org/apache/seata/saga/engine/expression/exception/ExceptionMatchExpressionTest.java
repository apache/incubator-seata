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
package org.apache.seata.saga.engine.expression.exception;

import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for {@link ExceptionMatchExpression}
 */
public class ExceptionMatchExpressionTest {

    @Test
    public void getValueWhenExactClassNameMatchReturnTrueTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        expr.setExpressionString("java.lang.RuntimeException");
        Object result = expr.getValue(new RuntimeException("test"));
        assertEquals(true, result);
    }

    @Test
    public void getValueWhenSubclassMatchReturnTrueTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        expr.setExpressionString("java.lang.Exception");
        Object result = expr.getValue(new RuntimeException("test"));
        assertEquals(true, result);
    }

    @Test
    public void getValueWhenNoMatchReturnFalseTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        expr.setExpressionString("java.io.IOException");
        Object result = expr.getValue(new RuntimeException("test"));
        assertEquals(false, result);
    }

    @Test
    public void getValueWhenContextIsNotExceptionReturnFalseTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        expr.setExpressionString("java.lang.RuntimeException");
        Object result = expr.getValue("not an exception");
        assertEquals(false, result);
    }

    @Test
    public void getValueWhenExpressionStringEmptyReturnFalseTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        // expressionString is null by default
        Object result = expr.getValue(new RuntimeException());
        assertEquals(false, result);
    }

    @Test
    public void getValueWhenContextIsNullReturnFalseTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        expr.setExpressionString("java.lang.RuntimeException");
        Object result = expr.getValue(null);
        assertEquals(false, result);
    }

    @Test
    public void setExpressionStringWithValidExceptionClassSetClassTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        expr.setExpressionString("java.lang.RuntimeException");
        assertEquals("java.lang.RuntimeException", expr.getExpressionString());
    }

    @Test
    public void setExpressionStringWithIOExceptionClassTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        expr.setExpressionString("java.io.IOException");
        assertEquals("java.io.IOException", expr.getExpressionString());

        // Test matching
        Object result = expr.getValue(new IOException("test"));
        assertEquals(true, result);
    }

    @Test
    public void setExpressionStringWithInvalidClassThrowEngineExecutionExceptionTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        assertThrows(EngineExecutionException.class, () -> expr.setExpressionString("com.nonexistent.NotAnException"));
    }

    @Test
    public void setValueDoNothingTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        // setValue is a no-op, just verify it doesn't throw
        assertDoesNotThrow(() -> expr.setValue("value", "context"));
    }

    @Test
    public void getExpressionStringWhenNotSetReturnNullTest() {
        ExceptionMatchExpression expr = new ExceptionMatchExpression();
        assertNull(expr.getExpressionString());
    }
}
