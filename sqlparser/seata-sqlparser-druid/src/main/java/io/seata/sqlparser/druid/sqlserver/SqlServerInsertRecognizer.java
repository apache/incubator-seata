package io.seata.sqlparser.druid.sqlserver;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.sqlserver.ast.stmt.SQLServerInsertStatement;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The type SqlServer insert recognizer.
 *
 * @author GoodBoyCoder
 */
public class SqlServerInsertRecognizer extends BaseSqlServerRecognizer implements SQLInsertRecognizer {
    private final SQLServerInsertStatement ast;

    /**
     * Instantiates a new sqlserver insert recognizer.
     *
     * @param originalSql the original sql
     */
    public SqlServerInsertRecognizer(String originalSql, SQLStatement ast) {
        super(originalSql);
        this.ast = (SQLServerInsertStatement) ast;
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
                list.add(((SQLIdentifierExpr) expr).getName());
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        if (ast.getTop() != null) {
            //deal with top sql
            dealTop(ast);
        }
        List<SQLInsertStatement.ValuesClause> valuesClauses = ast.getValuesList();
        List<List<Object>> rows = new ArrayList<>(valuesClauses.size());
        for (SQLInsertStatement.ValuesClause valuesClause : valuesClauses) {
            List<SQLExpr> exprList = valuesClause.getValues();
            List<Object> row = new ArrayList<>(exprList.size());
            rows.add(row);
            for (int i = 0, len = exprList.size(); i < len; i++) {
                SQLExpr expr = exprList.get(i);
                if (expr instanceof SQLNullExpr) {
                    row.add(Null.get());
                } else if (expr instanceof SQLValuableExpr) {
                    row.add(((SQLValuableExpr) expr).getValue());
                } else if (expr instanceof SQLVariantRefExpr) {
                    //add '?'
                    row.add(((SQLVariantRefExpr) expr).getName());
                } else if (expr instanceof SQLMethodInvokeExpr) {
                    row.add(SqlMethodExpr.get());
                } else if (expr instanceof SQLDefaultExpr) {
                    row.add(SqlDefaultExpr.get());
                } else if (expr instanceof SQLSequenceExpr) {
                    //Supported only since 2012 version of SQL Server,use next value for
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
        return null;
    }

    @Override
    public List<String> getDuplicateKeyUpdate() {
        return null;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }

    @Override
    public String getTableAlias() {
        return ast.getAlias();
    }

    @Override
    public String getTableName() {
        StringBuilder sb = new StringBuilder();
        SQLServerOutputVisitor visitor = new SQLServerOutputVisitor(sb) {
            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit(ast.getTableSource());
        return sb.toString();
    }
}
