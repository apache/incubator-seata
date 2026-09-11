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
package io.seata.sqlparser.druid.mysql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import io.seata.sqlparser.JoinRecognizer;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;

/**
 * The type My sql update recognizer.
 *
 * @author sharajava
 */
public class MySQLUpdateRecognizer extends BaseMySQLRecognizer implements SQLUpdateRecognizer, JoinRecognizer {

    private final MySqlUpdateStatement ast;

    private final Map<String, String> tableName2AliasMap = new HashMap<>(4);

    /**
     * Instantiates a new My sql update recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public MySQLUpdateRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (MySqlUpdateStatement)ast;
    }

    @Override
    public SQLType getSQLType() {
        SQLTableSource tableSource = this.ast.getTableSource();
        if (tableSource instanceof SQLExprTableSource) {
            return SQLType.UPDATE;
        } else if (tableSource instanceof SQLJoinTableSource) {
            return SQLType.UPDATE_JOIN;
        } else {
            throw new NotSupportYetException("not support update table source with unknow");
        }
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
                } else if (((SQLPropertyExpr)expr).getOwnerName().split("\\.").length > 1) {
                    list.add(((SQLPropertyExpr)expr).getOwnerName() + "." + ((SQLPropertyExpr)expr).getName());
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
    public String getTableAlias() {
        return ast.getTableSource().getAlias();
    }

    @Override
    public String getTableName() {
        SQLTableSource tableSource = this.ast.getTableSource();
        if (tableSource instanceof SQLExprTableSource) {
            return visitTableName((SQLExprTableSource) tableSource);
        } else if (tableSource instanceof SQLJoinTableSource) {
            //update join sql,like update t1 inner join t2 on t1.id = t2.id set name = ?, age = ?
            final int minTableNum = 2;
            StringBuilder joinTables = new StringBuilder();
            joinTables.append(tableSource.toString());
            tableName2AliasMap.put(tableSource.toString(), tableSource.getAlias());
            this.getTableNames(tableSource, joinTables);
            if (joinTables.toString().split(MULTI_TABLE_NAME_SEPERATOR).length < minTableNum + 1) {
                throw new ShouldNeverHappenException("should get at least two table name for update join table source:" + tableSource.toString());
            }
            //will return union table view name and single table names which linked by "#", like t1 inner join t2 on t1.id = t2.id#t1#t2
            return joinTables.toString();
        } else {
            throw new NotSupportYetException("not support the syntax of update with unknow");
        }
    }

    @Override
    public String getTableAlias(String tableName) {
        return tableName2AliasMap.get(tableName);
    }

    @Override
    public String getLimitCondition() {
        SQLLimit limit = ast.getLimit();
        return super.getLimitCondition(limit);
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        SQLLimit limit = ast.getLimit();
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
    public String getJoinCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        if (!(ast.getTableSource() instanceof SQLJoinTableSource)) {
            return "";
        }
        SQLExpr joinCondition = ((SQLJoinTableSource) ast.getTableSource()).getCondition();
        return super.getJoinCondition(joinCondition, parametersHolder, paramAppenderList);
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
    }

    private void getTableNames(SQLTableSource tableSource, StringBuilder tableNames) {
        if (tableSource instanceof SQLJoinTableSource) {
            //a:get left
            SQLTableSource left = ((SQLJoinTableSource) tableSource).getLeft();
            if (left instanceof SQLJoinTableSource) {
                this.getTableNames(left, tableNames);
            } else {
                tableNames.append(MULTI_TABLE_NAME_SEPERATOR);
                String tableName = visitTableName((SQLExprTableSource) left);
                tableNames.append(tableName);
                tableName2AliasMap.put(tableName, left.getAlias());
            }
            //b:get right
            SQLTableSource right = ((SQLJoinTableSource) tableSource).getRight();
            if (right instanceof SQLJoinTableSource) {
                this.getTableNames(right, tableNames);
            } else {
                tableNames.append(MULTI_TABLE_NAME_SEPERATOR);
                String tableName = visitTableName((SQLExprTableSource) right);
                tableNames.append(tableName);
                tableName2AliasMap.put(tableName, right.getAlias());
            }
        } else {
            tableNames.append(MULTI_TABLE_NAME_SEPERATOR);
            String tableName = visitTableName((SQLExprTableSource) tableSource);
            tableNames.append(tableName);
            tableName2AliasMap.put(tableName, tableSource.getAlias());
        }
    }

    private String visitTableName(SQLExprTableSource tableSource) {
        StringBuilder tableName = new StringBuilder();
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(tableName) {

            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit(tableSource);
        return tableName.toString();
    }
}
