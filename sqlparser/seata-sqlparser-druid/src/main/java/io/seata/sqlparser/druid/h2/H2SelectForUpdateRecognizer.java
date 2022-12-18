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
package io.seata.sqlparser.druid.h2;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
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

    @Override
    protected SQLStatement getAst() {
        return ast;
    }
}

