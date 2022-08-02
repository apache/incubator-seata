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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObjectImpl;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLSequenceExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.NotPlaceholderExpr;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;

/**
 * The type oracle insert recognizer.
 *
 * @author ccg
 */
public class OracleInsertRecognizer extends BaseOracleRecognizer implements SQLInsertRecognizer {

    private final OracleInsertStatement ast;

    private static final String PREFIX = "/*+";

    private static final String SUFFIX = "*/";

    private static final String IGNORE_HINT = "IGNORE_ROW_ON_DUPKEY_INDEX(";

    private static final char ESCAPE = '"';

    private String hintColumnName;

    /**
     * Instantiates a new My sql insert recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public OracleInsertRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (OracleInsertStatement)ast;
        this.hintColumnName = getHintColumn();
    }

    @Override
    public SQLType getSQLType() {
        return ast.getQuery() != null ? SQLType.INSERT_SELECT :
                StringUtils.isNotBlank(hintColumnName) ? SQLType.INSERT_IGNORE : SQLType.INSERT;
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
        visitor.visit(ast.getTableSource());
        return sb.toString();
    }

    @Override
    public boolean insertColumnsIsEmpty() {
        return CollectionUtils.isEmpty(ast.getColumns());
    }

    @Override
    public List<String> getInsertColumns() {
        List<SQLExpr> columnSQLExprs = ast.getColumns();
        if (columnSQLExprs.isEmpty()) {
            // INSERT INTO ta VALUES (...), without fields clarified
            return null;
        }
        List<String> list = new ArrayList<>(columnSQLExprs.size());
        for (SQLExpr expr : columnSQLExprs) {
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr)expr).getName());
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        List<SQLInsertStatement.ValuesClause> valuesClauses = ast.getValuesList();
        List<List<Object>> rows = new ArrayList<>(valuesClauses.size());
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
        List<SQLInsertStatement.ValuesClause> valuesList = ast.getValuesList();
        List<String> list = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause m : valuesList) {
            String values = m.toString().replace("VALUES", "").trim();
            // when all params is constant, the length of values less than 1
            if (values.length() > 1) {
                values = values.substring(1, values.length() - 1);
            }
            list.add(values);
        }
        return list;
    }

    @Override
    public List<String> getDuplicateKeyUpdate() {
        return null;
    }

    @Override
    public String getQuerySQL() {
        return Optional.ofNullable(ast.getQuery()).map(SQLObjectImpl::toString).orElse(null);
    }

    @Override
    public String getHintColumnName() {
        return hintColumnName;
    }

    @Override
    public boolean isIgnore() {
        return StringUtils.isNotBlank(hintColumnName);
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }

    /**
     * get hint column name
     *
     * @return column name
     */
    private String getHintColumn() {
        AtomicReference<String> columnName = new AtomicReference<>();
        ast.getHints().forEach(sqlHint -> {
            String hint = sqlHint.toString();
            if (hint.startsWith(PREFIX) && hint.endsWith(SUFFIX)) {
                hint = hint.replaceAll(" ", "");
                StringBuilder matchHint = new StringBuilder(IGNORE_HINT);
                int startIndex = hint.indexOf(matchHint.toString());
                if (startIndex != -1) {
                    int nextStartIndex = hint.indexOf("(", startIndex + matchHint.length());
                    String tableName = hint.substring(startIndex + matchHint.length(), nextStartIndex);
                    if (!getTableName().equals(tableName) && !(ESCAPE + getTableName() + ESCAPE).equals(tableName)) {
                        throw new ShouldNeverHappenException("in IGNORE_ROW_ON_DUPKEY_INDEX hint,the table name should as same as what you insert");
                    }
                    int endIndex = hint.indexOf(")", nextStartIndex + 1);
                    if (endIndex != -1) {
                        columnName.set(hint.substring(nextStartIndex + 1, endIndex));
                    }
                }
            }
        });
        return columnName.get();
    }
}
