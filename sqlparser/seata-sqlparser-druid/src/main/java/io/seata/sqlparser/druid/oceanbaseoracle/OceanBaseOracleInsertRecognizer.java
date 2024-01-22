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
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleInsertStatement;
import io.seata.sqlparser.util.ColumnUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Insert recognizer for OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleInsertRecognizer extends BaseOceanBaseOracleInsertRecognizer {
    private final OracleInsertStatement ast;

    public OceanBaseOracleInsertRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (OracleInsertStatement) ast;
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }

    @Override
    protected SQLTableSource getTableSource() {
        return ast.getTableSource();
    }

    @Override
    protected List<SQLExpr> getColumnsExprList() {
        return ast.getColumns();
    }

    @Override
    protected List<String> handleEmptyColumns(List<SQLExpr> columnExprList) {
        // TODO support insert into sub-query
        if (ast.getQuery() != null) {
            List<String> columns = new ArrayList<>();
            getInsertSelectColumns(ast.getQuery().getQuery(), columns);
            return columns;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    protected List<SQLInsertStatement.ValuesClause> getValuesClauses() {
        return ast.getValuesList();
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
