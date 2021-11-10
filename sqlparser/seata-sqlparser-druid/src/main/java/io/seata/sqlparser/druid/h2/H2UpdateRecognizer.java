package io.seata.sqlparser.druid.h2;

/**
 * @author hongyan
 * @date Created in 2021-11-09 17:07
 * @description
 */
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.h2.visitor.H2OutputVisitor;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.sqlparser.druid.BaseRecognizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class H2UpdateRecognizer extends BaseH2Recognizer implements SQLUpdateRecognizer {
    private SQLUpdateStatement ast;

    public H2UpdateRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (SQLUpdateStatement)ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.UPDATE;
    }

    @Override
    public List<String> getUpdateColumns() {
        List<SQLUpdateSetItem> updateSetItems = this.ast.getItems();
        List<String> list = new ArrayList(updateSetItems.size());
        Iterator var3 = updateSetItems.iterator();

        while(var3.hasNext()) {
            SQLUpdateSetItem updateSetItem = (SQLUpdateSetItem)var3.next();
            SQLExpr expr = updateSetItem.getColumn();
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr)expr).getName());
            } else {
                if (!(expr instanceof SQLPropertyExpr)) {
                    throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
                }

                SQLExpr owner = ((SQLPropertyExpr)expr).getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    list.add(((SQLIdentifierExpr)owner).getName() + "." + ((SQLPropertyExpr)expr).getName());
                }
            }
        }

        return list;
    }

    @Override
    public List<Object> getUpdateValues() {
        List<SQLUpdateSetItem> updateSetItems = this.ast.getItems();
        List<Object> list = new ArrayList(updateSetItems.size());
        Iterator var3 = updateSetItems.iterator();

        while(var3.hasNext()) {
            SQLUpdateSetItem updateSetItem = (SQLUpdateSetItem)var3.next();
            SQLExpr expr = updateSetItem.getValue();
            if (expr instanceof SQLValuableExpr) {
                list.add(((SQLValuableExpr)expr).getValue());
            } else {
                if (!(expr instanceof SQLVariantRefExpr)) {
                    throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
                }

                list.add(new BaseRecognizer.VMarker());
            }
        }

        return list;
    }

    @Override
    public String getWhereCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        SQLExpr where = this.ast.getWhere();
        return super.getWhereCondition(where, parametersHolder, paramAppenderList);
    }

    @Override
    public String getWhereCondition() {
        SQLExpr where = this.ast.getWhere();
        return super.getWhereCondition(where);
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
        SQLExprTableSource tableSource = (SQLExprTableSource)this.ast.getTableSource();
        visitor.visit(tableSource);
        return sb.toString();
    }

    @Override
    public String getLimit(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return super.getLimit(this.ast, this.getSQLType(), parametersHolder, paramAppenderList);
    }

    @Override
    public String getOrderBy() {
        return super.getOrderBy(this.ast, this.getSQLType());
    }
}

