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
import com.alibaba.druid.sql.ast.expr.SQLDefaultExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLSequenceExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.sqlserver.ast.stmt.SQLServerUpdateStatement;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.SQLUpdateRecognizer;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.struct.SqlDefaultExpr;
import org.apache.seata.sqlparser.struct.SqlMethodExpr;
import org.apache.seata.sqlparser.struct.SqlSequenceExpr;
import org.apache.seata.sqlparser.util.ColumnUtils;

/**
 * The type SqlServer update recognizer.
 *
 */
public class SqlServerUpdateRecognizer extends BaseSqlServerRecognizer implements SQLUpdateRecognizer {
    private SQLServerUpdateStatement ast;

    /**
     * Instantiates a new sqlserver update recognizer.
     *
     * @param originalSql the original sql
     */
    public SqlServerUpdateRecognizer(String originalSql, SQLStatement ast) {
        super(originalSql);
        this.ast = (SQLServerUpdateStatement) ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.UPDATE;
    }

    @Override
    public String getTableAlias() {
        return ast.getTableSource().getAlias();
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
                throw new NotSupportYetException("not support the syntax of update with join table");
            }
        };

        SQLTableSource tableSource = ast.getTableSource();
        if (tableSource instanceof SQLExprTableSource) {
            visitor.visit((SQLExprTableSource) tableSource);
        } else if (tableSource instanceof SQLJoinTableSource) {
            visitor.visit((SQLJoinTableSource) tableSource);
        } else {
            throw new NotSupportYetException("not support the syntax of update with unknow");
        }
        return sb.toString();
    }

    @Override
    public List<String> getUpdateColumns() {
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<String> list = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            SQLExpr expr = updateSetItem.getColumn();
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr) expr).getName());
            } else if (expr instanceof SQLPropertyExpr) {
                // This is alias case, like UPDATE xxx_tbl a SET a.name = ? WHERE a.id = ?
                SQLExpr owner = ((SQLPropertyExpr) expr).getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    list.add(((SQLIdentifierExpr) owner).getName() + "." + ((SQLPropertyExpr) expr).getName());
                    //This is table Field Full path, like update xxx_database.xxx_tbl set xxx_database.xxx_tbl.xxx_field...
                } else if (((SQLPropertyExpr) expr).getOwnerName().split("\\.").length > 1) {
                    list.add(((SQLPropertyExpr) expr).getOwnerName() + "." + ((SQLPropertyExpr) expr).getName());
                }
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public List<Object> getUpdateValues() {
        if (ast.getTop() != null) {
            //deal with top sql
            dealTop(ast);
        }
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<Object> list = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            SQLExpr expr = updateSetItem.getValue();
            if (expr instanceof SQLNullExpr) {
                list.add(Null.get());
            } else if (expr instanceof SQLValuableExpr) {
                list.add(((SQLValuableExpr) expr).getValue());
            } else if (expr instanceof SQLVariantRefExpr) {
                //add '?'
                list.add(((SQLVariantRefExpr) expr).getName());
            } else if (expr instanceof SQLMethodInvokeExpr) {
                list.add(SqlMethodExpr.get());
            } else if (expr instanceof SQLDefaultExpr) {
                list.add(SqlDefaultExpr.get());
            } else if (expr instanceof SQLSequenceExpr) {
                //Supported only since 2012 version of SQL Server,use next value for
                SQLSequenceExpr sequenceExpr = (SQLSequenceExpr) expr;
                String sequence = sequenceExpr.getSequence().getSimpleName();
                String function = sequenceExpr.getFunction().name;
                list.add(new SqlSequenceExpr(sequence, function));
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return list;
    }

    @Override
    public String getTableAlias(String tableName) {
        return SQLUpdateRecognizer.super.getTableAlias(tableName);
    }

    @Override
    public List<String> getUpdateColumnsUnEscape() {
        List<String> updateColumns = getUpdateColumns();
        return ColumnUtils.delEscape(updateColumns, getDbType());
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
