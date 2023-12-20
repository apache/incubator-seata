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

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement.InsertIntoClause;
import io.seata.sqlparser.util.ColumnUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Multi insert item recognizer for OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleMultiInsertItemRecognizer extends BaseOceanBaseOracleInsertRecognizer {
    private final OracleMultiInsertStatement ast;
    private final InsertIntoClause item;
    private final String conditionSQL;

    public OceanBaseOracleMultiInsertItemRecognizer(String originalSQL, SQLStatement ast,
                                                    InsertIntoClause item, String conditionSQL) {
        super(originalSQL);
        this.ast = (OracleMultiInsertStatement) ast;
        this.item = item;
        this.conditionSQL = conditionSQL;
    }

    public String getConditionSQL() {
        return conditionSQL;
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }

    @Override
    protected SQLTableSource getTableSource() {
        return item.getTableSource();
    }

    @Override
    protected List<SQLExpr> getColumnsExprList() {
        return item.getColumns();
    }

    @Override
    protected List<String> handleEmptyColumns(List<SQLExpr> columnExprList) {
        // TODO support insert into sub-query
        if (item.getQuery() != null) {
            List<String> columns = new ArrayList<>();
            getInsertSelectColumns(item.getQuery().getQuery(), columns);
            return columns;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<SQLInsertStatement.ValuesClause> getValuesClauses() {
        return item.getValuesList();
    }

    @Override
    protected List<List<Object>> handleEmptyValues(List<SQLInsertStatement.ValuesClause> valuesClauses,
                                                   Collection<Integer> primaryKeyIndex) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getInsertColumnsUnEscape() {
        List<String> insertColumns = getInsertColumns();
        return ColumnUtils.delEscape(insertColumns, getDbType());
    }
}