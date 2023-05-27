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
package io.seata.sqlparser.druid.sqlserver;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLDeleteRecognizer;
import io.seata.sqlparser.SQLType;

/**
 * The type SqlServer delete recognizer.
 *
 * @author GoodBoyCoder
 */
public class SqlServerDeleteRecognizer extends BaseSqlServerRecognizer implements SQLDeleteRecognizer {
    private final SQLDeleteStatement ast;

    /**
     * Instantiates a new sqlserver base recognizer.
     *
     * @param originalSql the original sql
     */
    public SqlServerDeleteRecognizer(String originalSql, SQLStatement statement) {
        super(originalSql);
        ast = (SQLDeleteStatement) statement;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.DELETE;
    }

    @Override
    public String getTableAlias() {
        //there is different between getFrom() and getTableSource()
        //when use "delete t1 from t t2", getTableSource().getAlias() will return null
        if (ast.getFrom() == null) {
            return ast.getTableSource().getAlias();
        }
        return ast.getFrom().getAlias();
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

            @Override
            public boolean visit(SQLJoinTableSource x) {
                throw new NotSupportYetException("not support the syntax of delete with join table");
            }
        };
        SQLTableSource tableSource;
        if (ast.getFrom() == null) {
            tableSource = ast.getTableSource();
        } else {
            tableSource = ast.getFrom();
        }

        if (tableSource instanceof SQLExprTableSource) {
            visitor.visit((SQLExprTableSource) tableSource);
        } else if (tableSource instanceof SQLJoinTableSource) {
            visitor.visit((SQLJoinTableSource) tableSource);
        } else {
            throw new NotSupportYetException("not support the syntax of delete with unknow");
        }
        return sb.toString();
    }

    @Override
    public String getWhereCondition(final ParametersHolder parametersHolder,
                                    final ArrayList<List<Object>> paramAppenderList) {
        SQLExpr where = ast.getWhere();
        return super.getWhereCondition(where, parametersHolder, paramAppenderList);
    }

    @Override
    public String getWhereCondition() {
        SQLExpr where = ast.getWhere();
        return super.getWhereCondition(where);
    }

    @Override
    public String getLimitCondition() {
        return null;
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override
    public String getOrderByCondition() {
        return null;
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        return null;
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }
}
