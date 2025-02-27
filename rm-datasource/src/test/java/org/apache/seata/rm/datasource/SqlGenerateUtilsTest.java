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
package org.apache.seata.rm.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;


class SqlGenerateUtilsTest {


    @Test
    void testBuildWhereConditionListByPKs() {
        List<String> pkNameList = new ArrayList<>();
        pkNameList.add("id");
        pkNameList.add("name");
        List<SqlGenerateUtils.WhereSql> results1 = SqlGenerateUtils.buildWhereConditionListByPKs(pkNameList, 4, "mysql", 2);
        Assertions.assertEquals(2, results1.size());
        results1.forEach(result -> {
            Assertions.assertEquals("(id,name) in ( (?,?),(?,?) )", result.getSql());
            Assertions.assertEquals(2, result.getRowSize());
            Assertions.assertEquals(2, result.getPkSize());
        });
        List<SqlGenerateUtils.WhereSql> results2 = SqlGenerateUtils.buildWhereConditionListByPKs(pkNameList, 5, "mysql", 2);
        Assertions.assertEquals(3, results2.size());
        Assertions.assertEquals("(id,name) in ( (?,?),(?,?) )", results2.get(0).getSql());
        Assertions.assertEquals(2, results2.get(0).getRowSize());
        Assertions.assertEquals(2, results2.get(0).getPkSize());
        Assertions.assertEquals("(id,name) in ( (?,?),(?,?) )", results2.get(1).getSql());
        Assertions.assertEquals("(id,name) in ( (?,?) )", results2.get(2).getSql());
        Assertions.assertEquals(1, results2.get(2).getRowSize());
        Assertions.assertEquals(2, results2.get(2).getPkSize());
    }

    @Test
    void testBuildSQLByPKs() {
        String sqlPrefix = "select id,name from t_order where ";
        List<String> pkNameList = new ArrayList<>();
        pkNameList.add("id");
        pkNameList.add("name");
        List<SqlGenerateUtils.WhereSql> whereList = SqlGenerateUtils.buildWhereConditionListByPKs(pkNameList, 4, "mysql", 2);
        StringJoiner sqlJoiner = new StringJoiner(" union ");
        whereList.forEach(whereSql -> sqlJoiner.add(sqlPrefix + " " + whereSql.getSql()));
        Assertions.assertEquals("select id,name from t_order where  (id,name) in ( (?,?),(?,?) ) union select id,name from t_order where  (id,name) in ( (?,?),(?,?) )", sqlJoiner.toString());
    }
}
