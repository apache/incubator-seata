package io.seata.sqlparser.druid.h2;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.h2.visitor.H2OutputVisitor;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.sqlparser.SQLType;
import java.util.ArrayList;
import java.util.List;
/**
 * @author hongyan
 * @date Created in 2021-11-09 17:07
 * @description
 */
public class H2SelectForUpdateRecognizer extends BaseH2Recognizer implements SQLSelectRecognizer {
    private final SQLSelectStatement ast;

    public H2SelectForUpdateRecognizer(String originalSql, SQLStatement ast) {
        super(originalSql);
        this.ast = (SQLSelectStatement)ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.SELECT_FOR_UPDATE;
    }

    @Override
    public String getWhereCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        SQLSelectQueryBlock selectQueryBlock = this.getSelect();
        SQLExpr where = selectQueryBlock.getWhere();
        return super.getWhereCondition(where, parametersHolder, paramAppenderList);
    }

    @Override
    public String getWhereCondition() {
        SQLSelectQueryBlock selectQueryBlock = this.getSelect();
        SQLExpr where = selectQueryBlock.getWhere();
        return super.getWhereCondition(where);
    }

    private SQLSelectQueryBlock getSelect() {
        SQLSelect select = this.ast.getSelect();
        if (select == null) {
            throw new SQLParsingException("should never happen!");
        } else {
            SQLSelectQueryBlock selectQueryBlock = select.getQueryBlock();
            if (selectQueryBlock == null) {
                throw new SQLParsingException("should never happen!");
            } else {
                return selectQueryBlock;
            }
        }
    }

    @Override
    public String getTableAlias() {
        SQLSelectQueryBlock selectQueryBlock = this.getSelect();
        SQLTableSource tableSource = selectQueryBlock.getFrom();
        return tableSource.getAlias();
    }

    @Override
    public String getTableName() {
        SQLSelectQueryBlock selectQueryBlock = this.getSelect();
        SQLTableSource tableSource = selectQueryBlock.getFrom();
        StringBuilder sb = new StringBuilder();
        H2OutputVisitor visitor = new H2OutputVisitor(sb) {
            @Override
            public boolean visit(SQLExprTableSource x) {
                this.printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit((SQLExprTableSource)tableSource);
        return sb.toString();
    }


    @Override
    public String getLimitCondition() {
        SQLLimit limit = getSelect().getLimit();
        return super.getLimitCondition(limit);
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        SQLLimit limit = getSelect().getLimit();
        return super.getLimitCondition(limit, parametersHolder, paramAppenderList);
    }

    @Override
    public String getOrderByCondition() {
        SQLOrderBy sqlOrderBy = getSelect().getOrderBy();
        return super.getOrderByCondition(sqlOrderBy);
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        SQLOrderBy sqlOrderBy = getSelect().getOrderBy();
        return super.getOrderByCondition(sqlOrderBy, parametersHolder, paramAppenderList);
    }
}

