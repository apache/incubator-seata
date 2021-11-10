package io.seata.sqlparser.druid.h2;

/**
 * @author hongyan
 * @date Created in 2021-11-09 17:04
 * @description
 */

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.h2.visitor.H2OutputVisitor;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLDeleteRecognizer;
import io.seata.sqlparser.SQLType;

import java.util.ArrayList;
import java.util.List;

public class H2DeleteRecognizer extends BaseH2Recognizer implements SQLDeleteRecognizer {
    private final SQLDeleteStatement ast;

    public H2DeleteRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (SQLDeleteStatement)ast;
    }
    @Override
    public SQLType getSQLType() {
        return SQLType.DELETE;
    }
    @Override
    public String getTableAlias() {
        return this.ast.getFrom() == null ? this.ast.getTableSource().getAlias() : this.ast.getFrom().getAlias();
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
        if (this.ast.getFrom() == null) {
            visitor.visit((SQLExprTableSource)this.ast.getTableSource());
        } else {
            visitor.visit((SQLExprTableSource)this.ast.getFrom());
        }

        return sb.toString();
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
    public String getLimit(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return super.getLimit(this.ast, this.getSQLType(), parametersHolder, paramAppenderList);
    }
    @Override
    public String getOrderBy() {
        return super.getOrderBy(this.ast, this.getSQLType());
    }
}
