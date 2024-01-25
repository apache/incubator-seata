/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.sqlparser.druid.sqlserver;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.sqlserver.ast.SQLServerSelectQueryBlock;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLSelectRecognizer;
import org.apache.seata.sqlparser.SQLType;

/**
 * The type SqlServer select for update recognizer.
 *
 */
public class SqlServerSelectForUpdateRecognizer extends BaseSqlServerRecognizer implements SQLSelectRecognizer {

    private final SQLSelectStatement ast;

    /**
     * Instantiates a new sqlserver select for update recognizer.
     *
     * @param originalSql the original sql
     */
    public SqlServerSelectForUpdateRecognizer(String originalSql, SQLStatement ast) {
        super(originalSql);
        this.ast = (SQLSelectStatement) ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.SELECT_FOR_UPDATE;
    }

    private SQLServerSelectQueryBlock getSelect() {
        SQLSelect select = ast.getSelect();
        if (select == null) {
            throw new SQLParsingException("should never happen!");
        }
        SQLServerSelectQueryBlock selectQueryBlock = (SQLServerSelectQueryBlock) select.getQueryBlock();
        if (selectQueryBlock == null) {
            throw new SQLParsingException("should never happen!");
        }
        if (selectQueryBlock.getTop() != null) {
            //deal with top sql
            dealTop(ast);
        }
        return selectQueryBlock;
    }

    @Override
    public String getTableAlias() {
        SQLSelectQueryBlock selectQueryBlock = getSelect();
        SQLTableSource tableSource = selectQueryBlock.getFrom();
        return tableSource.getAlias();
    }

    @Override
    public String getTableName() {
        SQLSelectQueryBlock selectQueryBlock = getSelect();
        SQLTableSource tableSource = selectQueryBlock.getFrom();
        StringBuilder sb = new StringBuilder();
        SQLServerOutputVisitor visitor = new SQLServerOutputVisitor(sb) {
            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit((SQLExprTableSource) tableSource);
        return sb.toString();
    }

    @Override
    public String getWhereCondition(ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList) {
        SQLSelectQueryBlock selectQueryBlock = getSelect();
        SQLExpr where = selectQueryBlock.getWhere();
        return super.getWhereCondition(where, parametersHolder, paramAppenderList);
    }

    @Override
    public String getWhereCondition() {
        SQLSelectQueryBlock selectQueryBlock = getSelect();
        SQLExpr where = selectQueryBlock.getWhere();
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
