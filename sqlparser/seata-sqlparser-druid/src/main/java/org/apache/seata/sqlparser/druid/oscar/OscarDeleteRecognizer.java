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
package org.apache.seata.sqlparser.druid.oscar;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLDeleteRecognizer;
import org.apache.seata.sqlparser.SQLType;

import java.util.ArrayList;
import java.util.List;

/**
 * The type oscar delete recognizer.
 *
 */
public class OscarDeleteRecognizer extends BaseOscarRecognizer implements SQLDeleteRecognizer {

    private final SQLDeleteStatement ast;

    /**
     * Instantiates a new Oscar sql delete recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public OscarDeleteRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (SQLDeleteStatement)ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.DELETE;
    }

    @Override
    public String getTableAlias() {
        return ast.getTableSource().getAlias();
    }

    @Override
    public String getTableName() {
        StringBuilder sb = new StringBuilder();
        OracleOutputVisitor visitor = new OracleOutputVisitor(sb) {

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
        //oscar does not support limit or rownum yet
        return null;
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        //oscar does not support limit or rownum yet
        return null;
    }

    @Override
    public String getOrderByCondition() {
        //oscar does not support order by yet
        return null;
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        //oscar does not support order by yet
        return null;
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }
}
