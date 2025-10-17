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
package org.apache.seata.sqlparser.druid.oracle;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLSequenceExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.sqlparser.SQLInsertRecognizer;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.NotPlaceholderExpr;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.struct.SqlMethodExpr;
import org.apache.seata.sqlparser.struct.SqlSequenceExpr;
import org.apache.seata.sqlparser.util.ColumnUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Oracle Multi Insert Recognizer for INSERT ALL statements
 */
public class OracleMultiInsertRecognizer extends BaseOracleRecognizer implements SQLInsertRecognizer {

    private final OracleMultiInsertStatement ast;
    private String validatedTableName;
    private List<String> validatedColumns;

    public OracleMultiInsertRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (OracleMultiInsertStatement) ast;
        validateSingleTableConsistency();
    }

    /**
     * Verifies that all entries are in the same table and have consistent column definitions.
     * <p>
     * If validation fails (e.g., conditional clauses are present, or table/column consistency is violated),
     * this method throws a {@link NotSupportYetException}.
     * <p>
     * Upon successful validation, initializes {@code validatedTableName} and {@code validatedColumns}
     * with the consistent table name and column definitions.
     */
    private void validateSingleTableConsistency() {
        if (CollectionUtils.isEmpty(ast.getEntries())) {
            return;
        }

        String firstTableName = null;
        List<String> firstColumns = null;

        for (OracleMultiInsertStatement.Entry entry : ast.getEntries()) {
            // Check whether conditional insertion is included. It is not supported yet.
            if (entry instanceof OracleMultiInsertStatement.ConditionalInsertClause) {
                throw new NotSupportYetException(
                        "Oracle Multi Insert with conditional clauses (WHEN...THEN) is not supported yet. " + "SQL: "
                                + getOriginalSQL());
            }
            if (entry instanceof OracleMultiInsertStatement.InsertIntoClause) {
                OracleMultiInsertStatement.InsertIntoClause insertClause =
                        (OracleMultiInsertStatement.InsertIntoClause) entry;

                // Get the current table name
                String currentTableName = getTableNameFromClause(insertClause);
                if (currentTableName == null) {
                    continue;
                }

                // Get current column information
                List<String> currentColumns = getColumnsFromClause(insertClause);

                if (firstTableName == null) {
                    firstTableName = currentTableName;
                    firstColumns = currentColumns;
                } else {
                    // Check whether the table names are consistent
                    if (!firstTableName.equalsIgnoreCase(currentTableName)) {
                        throw new NotSupportYetException(
                                "Oracle Multi Insert with different tables is not supported yet. " + "Found tables: "
                                        + firstTableName + " and " + currentTableName + ". SQL: "
                                        + getOriginalSQL());
                    }

                    // Check that column definitions are consistent
                    if (!Objects.equals(firstColumns, currentColumns)) {
                        throw new NotSupportYetException(
                                "Oracle Multi Insert with different column definitions is not supported yet. "
                                        + "Table: " + firstTableName + ". SQL: " + getOriginalSQL());
                    }
                }
            }
        }

        this.validatedTableName = firstTableName;
        this.validatedColumns = firstColumns;
    }

    private String getTableNameFromClause(OracleMultiInsertStatement.InsertIntoClause insertClause) {
        if (insertClause.getTableSource() != null) {
            StringBuilder sb = new StringBuilder();
            OracleOutputVisitor visitor = new OracleOutputVisitor(sb) {
                @Override
                public boolean visit(SQLExprTableSource x) {
                    printTableSourceExpr(x.getExpr());
                    return false;
                }
            };
            visitor.visit(insertClause.getTableSource());
            return sb.toString();
        }
        return null;
    }

    private List<String> getColumnsFromClause(OracleMultiInsertStatement.InsertIntoClause insertClause) {
        if (CollectionUtils.isEmpty(insertClause.getColumns())) {
            return Collections.emptyList();
        }

        List<String> columns = new ArrayList<>();
        for (SQLExpr expr : insertClause.getColumns()) {
            if (expr instanceof SQLIdentifierExpr) {
                columns.add(((SQLIdentifierExpr) expr).getName());
            } else {
                // Handling non-standard column names
                wrapSQLParsingException(expr);
            }
        }
        return columns;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }

    @Override
    public String getTableName() {
        return validatedTableName;
    }

    @Override
    public String getTableAlias() {
        if (!CollectionUtils.isEmpty(ast.getEntries())) {
            OracleMultiInsertStatement.Entry firstEntry = ast.getEntries().get(0);
            if (firstEntry instanceof OracleMultiInsertStatement.InsertIntoClause) {
                OracleMultiInsertStatement.InsertIntoClause insertClause =
                        (OracleMultiInsertStatement.InsertIntoClause) firstEntry;
                if (insertClause.getTableSource() != null) {
                    return insertClause.getTableSource().getAlias();
                }
            }
        }
        return null;
    }

    @Override
    public List<String> getInsertColumns() {
        return validatedColumns;
    }

    @Override
    public boolean insertColumnsIsEmpty() {
        return CollectionUtils.isEmpty(validatedColumns);
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        List<List<Object>> allRows = new ArrayList<>();

        if (!CollectionUtils.isEmpty(ast.getEntries())) {
            for (OracleMultiInsertStatement.Entry entry : ast.getEntries()) {
                if (entry instanceof OracleMultiInsertStatement.InsertIntoClause) {
                    OracleMultiInsertStatement.InsertIntoClause insertClause =
                            (OracleMultiInsertStatement.InsertIntoClause) entry;

                    if (!CollectionUtils.isEmpty(insertClause.getValuesList())) {
                        for (SQLInsertStatement.ValuesClause valuesClause : insertClause.getValuesList()) {
                            List<SQLExpr> exprs = valuesClause.getValues();
                            List<Object> row = new ArrayList<>(exprs.size());
                            allRows.add(row);

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
                                        wrapSQLParsingException(expr);
                                    }
                                    row.add(NotPlaceholderExpr.get());
                                }
                            }
                        }
                    }
                }
            }
        }
        return allRows;
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
    public List<String> getInsertColumnsUnEscape() {
        List<String> insertColumns = getInsertColumns();
        return ColumnUtils.delEscape(insertColumns, getDbType());
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }

    @Override
    public boolean isSqlSyntaxSupports() {
        return true;
    }
}
