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
import java.util.List;

/**
 * Oracle Multi Insert Recognizer for INSERT ALL statements
 */
public class OracleMultiInsertRecognizer extends BaseOracleRecognizer implements SQLInsertRecognizer {

    private final OracleMultiInsertStatement ast;

    public OracleMultiInsertRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (OracleMultiInsertStatement) ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }

    @Override
    public String getTableAlias() {
        // Oracle Multi Insert usually only one table is involved, take the table alias of the first inserted item
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
    public String getTableName() {
        // Oracle Multi Insert usually only one table is involved, take the table alias of the first inserted item
        if (!CollectionUtils.isEmpty(ast.getEntries())) {
            OracleMultiInsertStatement.Entry firstEntry = ast.getEntries().get(0);
            if (firstEntry instanceof OracleMultiInsertStatement.InsertIntoClause) {
                OracleMultiInsertStatement.InsertIntoClause insertClause =
                        (OracleMultiInsertStatement.InsertIntoClause) firstEntry;
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
            }
        }
        return null;
    }

    @Override
    public boolean insertColumnsIsEmpty() {
        if (!CollectionUtils.isEmpty(ast.getEntries())) {
            OracleMultiInsertStatement.Entry firstEntry = ast.getEntries().get(0);
            if (firstEntry instanceof OracleMultiInsertStatement.InsertIntoClause) {
                OracleMultiInsertStatement.InsertIntoClause insertClause =
                        (OracleMultiInsertStatement.InsertIntoClause) firstEntry;
                return CollectionUtils.isEmpty(insertClause.getColumns());
            }
        }
        return true;
    }

    @Override
    public List<String> getInsertColumns() {
        if (!CollectionUtils.isEmpty(ast.getEntries())) {
            OracleMultiInsertStatement.Entry firstEntry = ast.getEntries().get(0);
            if (firstEntry instanceof OracleMultiInsertStatement.InsertIntoClause) {
                OracleMultiInsertStatement.InsertIntoClause insertClause =
                        (OracleMultiInsertStatement.InsertIntoClause) firstEntry;
                if (!CollectionUtils.isEmpty(insertClause.getColumns())) {
                    List<SQLExpr> columnSQLExprs = insertClause.getColumns();
                    List<String> list = new ArrayList<>(columnSQLExprs.size());
                    for (SQLExpr expr : columnSQLExprs) {
                        if (expr instanceof SQLIdentifierExpr) {
                            list.add(((SQLIdentifierExpr) expr).getName());
                        } else {
                            wrapSQLParsingException(expr);
                        }
                    }
                    return list;
                }
            }
        }
        return null;
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
