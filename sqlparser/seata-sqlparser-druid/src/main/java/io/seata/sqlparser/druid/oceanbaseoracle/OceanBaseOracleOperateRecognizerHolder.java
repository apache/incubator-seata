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

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement.ConditionalInsertClause;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement.ConditionalInsertClauseItem;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement.Entry;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement.InsertIntoClause;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.druid.SQLOperateRecognizerHolder;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A recognizer holder as operation factory for OceanBaseOracle
 *
 * @author hsien999
 */
@LoadLevel(name = JdbcConstants.OCEANBASE_ORACLE)
public class OceanBaseOracleOperateRecognizerHolder implements SQLOperateRecognizerHolder {
    @Override
    public SQLRecognizer getDeleteRecognizer(String sql, SQLStatement ast) {
        return new OceanBaseOracleDeleteRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getInsertRecognizer(String sql, SQLStatement ast) {
        return new OceanBaseOracleInsertRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getUpdateRecognizer(String sql, SQLStatement ast) {
        return new OceanBaseOracleUpdateRecognizer(sql, ast);
    }

    @Override
    public SQLRecognizer getSelectForUpdateRecognizer(String sql, SQLStatement ast) {
        if (((SQLSelectStatement) ast).getSelect().getFirstQueryBlock().isForUpdate()) {
            return new OceanBaseOracleSelectForUpdateRecognizer(sql, ast);
        }
        return null;
    }

    /**
     * Support multi insert statement for OceanBase in oracle mode:
     * <p>multi_table_insert:
     * <p>INSERT { ALL { insert_into_clause } | conditional_insert_clause} subquery
     * <p>{ insert_into_clause }: like 'INTO tb1 VALUES(1, 2) INTO tb2 VALUES(3, 4)'
     * <p>{ conditional_insert_clause }: like 'WHEN col1 > 1 THEN {insert_into_clause}
     * WHEN col2 > 2 THEN {insert_into_clause} ELSE {insert_into_clause}'
     * <p>subquery: like 'SELECT col1, col2 from tb3'
     *
     * @param sql the sql string
     * @param ast the sql statement
     * @return list of sql recognizers
     */
    public List<SQLRecognizer> getMultiInsertStatement(String sql, SQLStatement ast) {
        List<SQLRecognizer> sqlRecognizers = new ArrayList<>();
        OracleMultiInsertStatement multiInsertStatement = (OracleMultiInsertStatement) ast;
        List<Entry> entries = multiInsertStatement.getEntries();
        if (CollectionUtils.isEmpty(entries)) {
            throw new ShouldNeverHappenException("Empty entries in multi insert statement: " + sql);
        }
        String whereExprStr = "WHERE", notExprStr = "NOT", orExprStr = "OR", forUpdateExprStr = "FOR UPDATE";
        Entry entry0 = entries.get(0);
        boolean notSupported = false;
        if (entry0 instanceof ConditionalInsertClause) {
            // if the entry is a conditional insert clause
            // e.g. INSERT ALL INTO WHEN col1 > 1 INTO tbl1 VALUES(1,2) WHEN col2 > 2 INTO tbl2 VALUES(1,2)
            //       ELSE INTO tbl3 VALUES(1,2) SELECT col1, col2 FROM tbl3;
            if (entries.size() == 1) {
                List<ConditionalInsertClauseItem> clauseItems = ((ConditionalInsertClause) entry0).getItems();
                List<String> conditionals = new ArrayList<>();
                String whenStr, whereStr;
                String subQueryCondStr, subQueryStr = multiInsertStatement.getSubQuery().toString();
                // {WHEN} items with condition expr in conditional insert clause
                for (ConditionalInsertClauseItem clauseItem : clauseItems) {
                    whenStr = clauseItem.getWhen().toString();
                    conditionals.add(whenStr);
                    whereStr = whereExprStr + " " + whenStr;
                    // the sub query with condition, e.g. SELECT col1, col2 FROM tbl3 WHERE col1 > 1 FOR UPDATE;
                    subQueryCondStr = subQueryStr + " " + whereStr + " " + forUpdateExprStr;
                    sqlRecognizers.add(new OceanBaseOracleMultiInsertItemRecognizer(sql, ast,
                        clauseItem.getThen(), subQueryCondStr));
                }
                // {ELSE} item in conditional insert clause
                InsertIntoClause elseItem = ((ConditionalInsertClause) entry0).getElseItem();
                if (elseItem != null) {
                    if (conditionals.size() == 1) {
                        whereStr = whereExprStr + " " + notExprStr + " (" + conditionals.get(0) + ")";
                    } else {
                        whereStr = conditionals.stream().collect(
                            Collectors.joining(") " + orExprStr + " (",
                                whereExprStr + " " + notExprStr + " ((",
                                "))")
                        );
                    }
                    // the sub query with condition,
                    // e.g. SELECT col1, col2 FROM tbl3 WHERE NOT ((col1 > 1) OR (col2 > 2)) FOR UPDATE;
                    subQueryCondStr = subQueryStr + " " + whereStr + " " + forUpdateExprStr;
                    sqlRecognizers.add(new OceanBaseOracleMultiInsertItemRecognizer(sql, ast, elseItem, subQueryCondStr));
                }
            } else {
                notSupported = true;
            }
        } else if (entry0 instanceof InsertIntoClause) {
            // else if it is an insert into clause
            // e.g. INSERT ALL INTO tbl1 VALUES(1,2) tbl2 VALUES(1,2)
            try {
                for (Entry entry : entries) {
                    InsertIntoClause intoItem = (InsertIntoClause) entry;
                    sqlRecognizers.add(new OceanBaseOracleMultiInsertItemRecognizer(sql, ast, intoItem, null));
                }
            } catch (ClassCastException e) {
                notSupported = true;
            }
        } else {
            notSupported = true;
        }
        if (notSupported) {
            throw new NotSupportYetException("Not supported clause in multi insert statement: " + sql);
        }
        return sqlRecognizers;
    }
}
