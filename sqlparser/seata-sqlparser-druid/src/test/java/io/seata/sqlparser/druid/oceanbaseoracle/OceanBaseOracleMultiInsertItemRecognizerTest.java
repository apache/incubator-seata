/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.sqlparser.druid.oceanbaseoracle;

import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.AbstractRecognizerTest;
import io.seata.sqlparser.struct.NotPlaceholderExpr;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlDefaultExpr;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for multi insert item recognizer of OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleMultiInsertItemRecognizerTest extends AbstractRecognizerTest {
    private final int pkIndex = 0;

    @Override
    public String getDbType() {
        return JdbcConstants.OCEANBASE_ORACLE;
    }

    private List<SQLRecognizer> getSQLStatements(String sql) {
        SQLRecognizerFactory recognizerFactory = new DruidDelegatingSQLRecognizerFactoryForTest();
        return recognizerFactory.create(sql, getDbType());
    }

    @Test
    public void testInsertIntoClause() {
        String sql = "INSERT ALL INTO t1 (id, name) VALUES (1, 'test') INTO t2 t_2 (id, name) VALUES (?, ?)";
        List<SQLRecognizer> recognizers = getSQLStatements(sql);
        Assertions.assertTrue(recognizers != null && recognizers.size() == 2);
        Assertions.assertTrue(recognizers.stream().allMatch(r -> r instanceof OceanBaseOracleMultiInsertItemRecognizer));

        OceanBaseOracleMultiInsertItemRecognizer recognizer1 = (OceanBaseOracleMultiInsertItemRecognizer) recognizers.get(0);
        Assertions.assertEquals(sql, recognizer1.getOriginalSQL());
        Assertions.assertEquals(SQLType.INSERT, recognizer1.getSQLType());
        Assertions.assertEquals("t1", recognizer1.getTableName());
        Assertions.assertNull(recognizer1.getTableAlias());
        Assertions.assertEquals(Arrays.asList("id", "name"), recognizer1.getInsertColumns());
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, "test")),
            recognizer1.getInsertRows(Collections.singletonList(pkIndex)));
        Assertions.assertNull(recognizer1.getConditionSQL());

        OceanBaseOracleMultiInsertItemRecognizer recognizer2 = (OceanBaseOracleMultiInsertItemRecognizer) recognizers.get(1);
        Assertions.assertEquals(sql, recognizer2.getOriginalSQL());
        Assertions.assertEquals(SQLType.INSERT, recognizer2.getSQLType());
        Assertions.assertEquals("t2", recognizer2.getTableName());
        Assertions.assertEquals("t_2", recognizer2.getTableAlias());
        Assertions.assertEquals(Arrays.asList("id", "name"), recognizer2.getInsertColumns());
        Assertions.assertEquals(Collections.singletonList(Arrays.asList("?", "?")),
            recognizer2.getInsertRows(Collections.singletonList(pkIndex)));
        Assertions.assertNull(recognizer2.getConditionSQL());
    }

    @Test
    public void testConditionalInsertClause() {
        String sql = "INSERT ALL \n" +
            "WHEN col1 > 1 THEN INTO t1 VALUES(1, 'test') \n" +
            "WHEN col2 > 1 THEN INTO t2 t_2 VALUES(2, 'test') \n" +
            "ELSE INTO t3 VALUES(id_seq.nextval, ?, 'test', null, sysdate(), default, xxx)" +
            "SELECT col1,col2 FROM t3;";
        List<SQLRecognizer> recognizers = getSQLStatements(sql);
        Assertions.assertTrue(recognizers != null && recognizers.size() == 3);
        Assertions.assertTrue(recognizers.stream().allMatch(r -> r instanceof OceanBaseOracleMultiInsertItemRecognizer));

        OceanBaseOracleMultiInsertItemRecognizer recognizer1 = (OceanBaseOracleMultiInsertItemRecognizer) recognizers.get(0);
        Assertions.assertEquals(sql, recognizer1.getOriginalSQL());
        Assertions.assertEquals(SQLType.INSERT, recognizer1.getSQLType());
        Assertions.assertEquals("t1", recognizer1.getTableName());
        Assertions.assertNull(recognizer1.getTableAlias());
        Assertions.assertTrue(CollectionUtils.isEmpty(recognizer1.getInsertColumns()));
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(1, "test")),
            recognizer1.getInsertRows(Collections.singletonList(pkIndex)));
        Assertions.assertEquals("SELECT col1, col2\nFROM t3 WHERE col1 > 1 FOR UPDATE", recognizer1.getConditionSQL());

        OceanBaseOracleMultiInsertItemRecognizer recognizer2 = (OceanBaseOracleMultiInsertItemRecognizer) recognizers.get(1);
        Assertions.assertEquals(sql, recognizer2.getOriginalSQL());
        Assertions.assertEquals(SQLType.INSERT, recognizer2.getSQLType());
        Assertions.assertEquals("t2", recognizer2.getTableName());
        Assertions.assertEquals("t_2", recognizer2.getTableAlias());
        Assertions.assertTrue(CollectionUtils.isEmpty(recognizer2.getInsertColumns()));
        Assertions.assertEquals(Collections.singletonList(Arrays.asList(2, "test")),
            recognizer2.getInsertRows(Collections.singletonList(pkIndex)));
        Assertions.assertEquals("SELECT col1, col2\nFROM t3 WHERE col2 > 1 FOR UPDATE", recognizer2.getConditionSQL());

        OceanBaseOracleMultiInsertItemRecognizer recognizer3 = (OceanBaseOracleMultiInsertItemRecognizer) recognizers.get(2);
        Assertions.assertEquals(sql, recognizer3.getOriginalSQL());
        Assertions.assertEquals(SQLType.INSERT, recognizer3.getSQLType());
        Assertions.assertEquals("t3", recognizer3.getTableName());
        Assertions.assertNull(recognizer3.getTableAlias());
        Assertions.assertTrue(CollectionUtils.isEmpty(recognizer3.getInsertColumns()));
        List<List<Object>> insertRows = recognizer3.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(1, insertRows.size());
        List<Object> insertRow = insertRows.get(0);
        SqlSequenceExpr sequence = (SqlSequenceExpr) (insertRow.get(0));
        Assertions.assertEquals("id_seq", sequence.getSequence());
        Assertions.assertEquals("NEXTVAL", sequence.getFunction());
        Assertions.assertEquals(Arrays.asList(
            "?", "test", Null.get(), SqlMethodExpr.get(), SqlDefaultExpr.get(), NotPlaceholderExpr.get()
        ), insertRow.subList(1, insertRow.size()));
        Assertions.assertEquals("SELECT col1, col2\nFROM t3 WHERE NOT ((col1 > 1) OR (col2 > 1)) FOR UPDATE",
            recognizer3.getConditionSQL());
    }
}
