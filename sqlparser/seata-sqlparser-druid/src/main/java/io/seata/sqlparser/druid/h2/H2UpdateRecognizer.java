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
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.h2.visitor.H2OutputVisitor;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hongyan
 * @date Created in 2021-11-09 17:07
 * @description
 */
public class H2UpdateRecognizer extends BaseH2Recognizer implements SQLUpdateRecognizer {
    private SQLUpdateStatement ast;

    public H2UpdateRecognizer(String originalSql, SQLStatement ast) {
        super(originalSql);
        this.ast = (SQLUpdateStatement)ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.UPDATE;
    }

    @Override
    public List<String> getUpdateColumns() {
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<String> list = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            SQLExpr expr = updateSetItem.getColumn();
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr)expr).getName());
            } else if (expr instanceof SQLPropertyExpr) {
                // This is alias case, like UPDATE xxx_tbl a SET a.name = ? WHERE a.id = ?
                SQLExpr owner = ((SQLPropertyExpr)expr).getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    list.add(((SQLIdentifierExpr)owner).getName() + "." + ((SQLPropertyExpr)expr).getName());
                    //This is table Field Full path, like update xxx_database.xxx_tbl set xxx_database.xxx_tbl.xxx_field...
                } else if (((SQLPropertyExpr) expr).getOwnerName().split("\\.").length > 1) {
                    list.add(((SQLPropertyExpr)expr).getOwnerName()  + "." + ((SQLPropertyExpr)expr).getName());
                }
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public List<Object> getUpdateValues() {
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<Object> list = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            SQLExpr expr = updateSetItem.getValue();
            if (expr instanceof SQLValuableExpr) {
                list.add(((SQLValuableExpr)expr).getValue());
            } else if (expr instanceof SQLVariantRefExpr) {
                list.add(new VMarker());
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public List<String> getUpdateColumnsIsSimplified() {
        List<String> updateColumns = getUpdateColumns();
        return ColumnUtils.delEscape(updateColumns, getDbType());
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
    public String getLimitCondition() {
        SQLLimit limit = null;
        return super.getLimitCondition(limit);
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        SQLLimit limit = null;
        return super.getLimitCondition(limit, parametersHolder, paramAppenderList);
    }

    @Override
    public String getOrderByCondition() {
        SQLOrderBy sqlOrderBy = ast.getOrderBy();
        return super.getOrderByCondition(sqlOrderBy);
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        SQLOrderBy sqlOrderBy = ast.getOrderBy();
        return super.getOrderByCondition(sqlOrderBy, parametersHolder, paramAppenderList);
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }

    public String getDbType() {
        return JdbcConstants.H2;
    }
}
