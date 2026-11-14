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
package org.apache.seata.sqlparser.druid.oceanbase;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.struct.Null;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OceanBaseConditionsBehaviorTest {
    private final ParametersHolder parameters =
            () -> Collections.singletonMap(1, new ArrayList<>(Arrays.asList("first", Null.get())));

    private static class Conditions extends OceanBaseUpdateRecognizer {
        Conditions() {
            super(
                    "update orders set amount = 1",
                    SQLUtils.parseStatements("update orders set amount = 1", "oceanbase")
                            .get(0));
        }

        String order(SQLOrderBy order) {
            return super.getOrderByCondition(order);
        }

        String order(SQLOrderBy order, ParametersHolder holder, ArrayList<List<Object>> values) {
            return super.getOrderByCondition(order, holder, values);
        }
    }

    @Test
    void orderByRetainsBatchBindingsAndHandlesAbsentClause() {
        Conditions conditions = new Conditions();
        ArrayList<List<Object>> values = new ArrayList<>();
        assertEquals("", conditions.order(null));
        assertEquals("", conditions.order(null, parameters, values));
        SQLOrderBy order = new SQLOrderBy();
        SQLVariantRefExpr parameter = new SQLVariantRefExpr("?");
        parameter.setIndex(0);
        order.addItem(parameter);
        assertEquals("ORDER BY ?", conditions.order(order));
        assertEquals("ORDER BY ?", conditions.order(order, parameters, values));
        assertEquals(Arrays.asList(Collections.singletonList("first"), Collections.singletonList(null)), values);
    }

    @Test
    void namedParameterIsRenderedWithoutConsumingPositionalBindings() {
        Conditions conditions = new Conditions();
        ArrayList<List<Object>> values = new ArrayList<>();
        SQLOrderBy order = new SQLOrderBy();
        order.addItem(new SQLVariantRefExpr(":named"));
        assertEquals("ORDER BY :named", conditions.order(order, parameters, values));
        assertTrue(values.isEmpty());
    }
}
