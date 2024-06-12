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
package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SqlSequenceExprTest {

    @Test
    public void testDefaultConstructor() {

        SqlSequenceExpr expr = new SqlSequenceExpr();

        assertNull(expr.getSequence(), "Initial sequence should be null.");
        assertNull(expr.getFunction(), "Initial function should be null.");
    }

    @Test
    public void testParameterizedConstructor() {

        SqlSequenceExpr expr = new SqlSequenceExpr("mySequence", "myFunction");

        assertEquals("mySequence", expr.getSequence(), "Sequence should be 'mySequence'.");
        assertEquals("myFunction", expr.getFunction(), "Function should be 'myFunction'.");
    }

    @Test
    public void testSetGetSequence() {

        SqlSequenceExpr expr = new SqlSequenceExpr();

        expr.setSequence("newSequence");
        assertEquals("newSequence", expr.getSequence(), "Sequence should be 'newSequence'.");
    }

    @Test
    public void testSetGetFunction() {

        SqlSequenceExpr expr = new SqlSequenceExpr();

        expr.setFunction("newFunction");
        assertEquals("newFunction", expr.getFunction(), "Function should be 'newFunction'.");
    }

}
