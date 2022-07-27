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
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Insert recognizer for OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleInsertRecognizer extends BaseOceanBaseOracleRecognizer implements SQLInsertRecognizer {
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
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }

    @Override
    public String getTableAlias() {
        return ast.getTableSource().getAlias();
    }

    @Override
    public String getTableName() {
        StringBuilder sb = new StringBuilder();
        OracleOutputVisitor visitor = new OracleOutputVisitor(sb) {

            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };

        // for insert: tableSource is a SQLExprTableSource
        visitor.visit(ast.getTableSource());
        return sb.toString();
    }

    @Override
    public boolean insertColumnsIsEmpty() {
        return CollectionUtils.isEmpty(ast.getColumns());
    }

    @Override
    public List<String> getInsertColumns() {
        List<SQLExpr> sqlExprList = ast.getColumns();
        if (CollectionUtils.isEmpty(sqlExprList)) {
            return null;
        }
        List<String> insertColumns = new ArrayList<>(sqlExprList.size());
        for (SQLExpr expr : sqlExprList) {
            // support only identifier expression in inserted columns
            if (expr instanceof SQLIdentifierExpr) {
                insertColumns.add(((SQLIdentifierExpr) expr).getName());
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return insertColumns;
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        List<SQLInsertStatement.ValuesClause> valuesClauses = ast.getValuesList();
        List<List<Object>> rows = new ArrayList<>(valuesClauses.size());
        for (SQLInsertStatement.ValuesClause valuesClause : valuesClauses) {
            List<SQLExpr> exprList = valuesClause.getValues();
            List<Object> row = new ArrayList<>(exprList.size());
            rows.add(row);
            for (int i = 0; i < exprList.size(); i++) {
                SQLExpr expr = exprList.get(i);
                // like: (null, 1, ?, sysdate(), default, seq.nextval)
                if (expr instanceof SQLNullExpr) {
                    row.add(Null.get());
                } else if (expr instanceof SQLValuableExpr) {
                    row.add(((SQLValuableExpr) expr).getValue());
                } else if (expr instanceof SQLVariantRefExpr) {
                    row.add(((SQLVariantRefExpr) expr).getName());
                } else if (expr instanceof SQLMethodInvokeExpr) {
                    row.add(SqlMethodExpr.get());
                } else if (expr instanceof SQLDefaultExpr) {
                    row.add(SqlDefaultExpr.get());
                } else if (expr instanceof SQLSequenceExpr) {
                    SQLSequenceExpr sequenceExpr = (SQLSequenceExpr) expr;
                    String sequence = sequenceExpr.getSequence().getSimpleName();
                    String function = sequenceExpr.getFunction().name;
                    row.add(new SqlSequenceExpr(sequence, function));
                } else {
                    if (primaryKeyIndex.contains(i)) {
                        wrapSQLParsingException(expr);
                    }
                    row.add(NotPlaceholderExpr.get());
                }
            }
        }
        return rows;
    }

    @Override
    public List<String> getInsertParamsValue() {
        List<String> valuesClauses = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause clause : ast.getValuesList()) {
            String values = clause.toString().replace("VALUES", "").trim();
            if (values.length() > 1) {
                values = values.substring(1, values.length() - 1);
            }
            valuesClauses.add(values);
        }
        return valuesClauses;
    }

    @Override
    public List<String> getDuplicateKeyUpdate() {
        return null;
    }
}
