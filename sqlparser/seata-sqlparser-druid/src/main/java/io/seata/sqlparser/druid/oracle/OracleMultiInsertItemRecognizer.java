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
package io.seata.sqlparser.druid.oracle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLSequenceExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.oracle.ast.OracleSQLObject;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.BaseRecognizer;
import io.seata.sqlparser.struct.NotPlaceholderExpr;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;



/**
 * The type oracle multi insert recognizer.
 *
 * @author renliangyu857
 */
public class OracleMultiInsertItemRecognizer extends BaseRecognizer implements SQLInsertRecognizer {

    private final OracleMultiInsertStatement ast;

    private final OracleSQLObject item;

    public OracleMultiInsertItemRecognizer(String originalSQL, SQLStatement ast, OracleSQLObject item) {
        super(originalSQL);
        this.ast = (OracleMultiInsertStatement) ast;
        this.item = item;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }

    @Override
    public String getTableAlias() {
        return ((OracleMultiInsertStatement.InsertIntoClause) ast.getEntries().get(0)).getAlias();
    }

    @Override
    public String getTableName() {
        StringBuilder tableName = new StringBuilder();
        if (this.item instanceof OracleMultiInsertStatement.ConditionalInsertClauseItem) {
            visitTableName(((OracleMultiInsertStatement.ConditionalInsertClauseItem) this.item).getThen().getTableSource(), tableName);

        } else if (this.item instanceof OracleMultiInsertStatement.InsertIntoClause) {
            visitTableName(((OracleMultiInsertStatement.InsertIntoClause) this.item).getTableSource(), tableName);
        } else {
            throw new ShouldNeverHappenException("the oracle multi insert clause is not exist");
        }
        return tableName.toString();
    }

    @Override
    public boolean insertColumnsIsEmpty() {
        return CollectionUtils.isEmpty(this.getInsertColumns());
    }

    @Override
    public List<String> getInsertColumns() {
        List<SQLExpr> columnSQLExprs = new ArrayList<>();
        if (this.item instanceof OracleMultiInsertStatement.ConditionalInsertClauseItem) {
            columnSQLExprs = ((OracleMultiInsertStatement.ConditionalInsertClauseItem) this.item).getThen().getColumns();
        } else if (this.item instanceof OracleMultiInsertStatement.InsertIntoClause) {
            columnSQLExprs = ((OracleMultiInsertStatement.InsertIntoClause) this.item).getColumns();
        }
        if (columnSQLExprs.isEmpty()) {
            List<String> columns = new ArrayList<>();
            this.getInsertSelectColumns(ast.getSubQuery().getQuery(),columns);
            return columns;
        }
        List<String> list = new ArrayList<>(columnSQLExprs.size());
        for (SQLExpr expr : columnSQLExprs) {
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr) expr).getName());
            } else {
                throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
            }
        }
        return list;
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        List<SQLInsertStatement.ValuesClause> valuesClauses = new ArrayList<>();
        if (this.item instanceof OracleMultiInsertStatement.ConditionalInsertClauseItem) {
            valuesClauses = ((OracleMultiInsertStatement.ConditionalInsertClauseItem) this.item).getThen().getValuesList();
        } else if (this.item instanceof OracleMultiInsertStatement.InsertIntoClause) {
            valuesClauses = ((OracleMultiInsertStatement.InsertIntoClause) this.item).getValuesList();
        }
        if (valuesClauses.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<Object>> rows = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause valuesClause : valuesClauses) {
            List<SQLExpr> exprs = valuesClause.getValues();
            List<Object> row = new ArrayList<>(exprs.size());
            rows.add(row);
            for (int i = 0, len = exprs.size(); i < len; i++) {
                SQLExpr expr = exprs.get(i);
                if (expr instanceof SQLNullExpr) {
                    row.add(Null.get());
                } else if (expr instanceof SQLValuableExpr) {
                    row.add(((SQLValuableExpr) expr).getValue());
                } else if (expr instanceof SQLVariantRefExpr) {
                    row.add(((SQLVariantRefExpr) expr).getName());
                } else if (expr instanceof SQLMethodInvokeExpr) {
                    row.add(SqlMethodExpr.get());
                } else if (expr instanceof SQLSequenceExpr) {
                    SQLSequenceExpr sequenceExpr = (SQLSequenceExpr) expr;
                    String sequence = sequenceExpr.getSequence().getSimpleName();
                    String function = sequenceExpr.getFunction().name;
                    row.add(new SqlSequenceExpr(sequence, function));
                } else {
                    if (primaryKeyIndex.contains(i)) {
                        throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
                    }
                    row.add(NotPlaceholderExpr.get());
                }
            }
        }
        return rows;
    }

    @Override
    public List<String> getInsertParamsValue() {
        return null;
    }

    @Override
    public List<String> getDuplicateKeyUpdate() {
        return null;
    }

    @Override
    public String getSubQuerySql() {
        return this.ast.getSubQuery().toString();
    }

    @Override
    protected SQLStatement getAst() {
        return this.ast;
    }

    private void visitTableName(SQLExprTableSource tableSource, StringBuilder tableName) {
        OracleOutputVisitor visitor = new OracleOutputVisitor(tableName) {

            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit(tableSource);
    }
}
