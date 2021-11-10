package io.seata.sqlparser.druid.h2;

/**
 * @author hongyan
 * @date Created in 2021-11-09 17:05
 * @description
 */

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement.ValuesClause;
import com.alibaba.druid.sql.dialect.h2.visitor.H2OutputVisitor;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.NotPlaceholderExpr;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class H2InsertRecognizer extends BaseH2Recognizer implements SQLInsertRecognizer {
    private final SQLInsertStatement ast;

    public H2InsertRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (SQLInsertStatement)ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }
    @Override
    public String getTableAlias() {
        return this.ast.getTableSource().getAlias();
    }
    @Override
    public String getTableName() {
        StringBuilder sb = new StringBuilder();
        H2OutputVisitor visitor = new H2OutputVisitor(sb) {
            @Override
            public boolean visit(SQLExprTableSource x) {
                this.printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit(this.ast.getTableSource());
        return sb.toString();
    }
    @Override
    public boolean insertColumnsIsEmpty() {
        return CollectionUtils.isEmpty(this.ast.getColumns());
    }
    @Override
    public List<String> getInsertColumns() {
        List<SQLExpr> columnSQLExprs = this.ast.getColumns();
        if (columnSQLExprs.isEmpty()) {
            return null;
        } else {
            List<String> list = new ArrayList(columnSQLExprs.size());
            Iterator var3 = columnSQLExprs.iterator();

            while(var3.hasNext()) {
                SQLExpr expr = (SQLExpr)var3.next();
                if (!(expr instanceof SQLIdentifierExpr)) {
                    throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
                }

                list.add(((SQLIdentifierExpr)expr).getName());
            }

            return list;
        }
    }
    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        List<ValuesClause> valuesClauses = this.ast.getValuesList();
        List<List<Object>> rows = new ArrayList(valuesClauses.size());
        Iterator var4 = valuesClauses.iterator();

        while(var4.hasNext()) {
            ValuesClause valuesClause = (ValuesClause)var4.next();
            List<SQLExpr> exprs = valuesClause.getValues();
            List<Object> row = new ArrayList(exprs.size());
            rows.add(row);
            int i = 0;

            for(int len = exprs.size(); i < len; ++i) {
                SQLExpr expr = (SQLExpr)exprs.get(i);
                if (expr instanceof SQLNullExpr) {
                    row.add(Null.get());
                } else if (expr instanceof SQLValuableExpr) {
                    row.add(((SQLValuableExpr)expr).getValue());
                } else if (expr instanceof SQLVariantRefExpr) {
                    row.add(((SQLVariantRefExpr)expr).getName());
                } else if (expr instanceof SQLMethodInvokeExpr) {
                    row.add(SqlMethodExpr.get());
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
}
