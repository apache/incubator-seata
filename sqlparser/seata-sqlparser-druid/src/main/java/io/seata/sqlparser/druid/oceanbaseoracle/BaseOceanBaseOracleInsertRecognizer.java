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
package io.seata.sqlparser.druid.oceanbaseoracle;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Base insert sql recognizer for OceanBaseOracle
 *
 * @author hsien999
 */
public abstract class BaseOceanBaseOracleInsertRecognizer extends BaseOceanBaseOracleRecognizer implements SQLInsertRecognizer {

    public BaseOceanBaseOracleInsertRecognizer(String originalSql) {
        super(originalSql);
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }

    @Override
    public boolean insertColumnsIsEmpty() {
        return CollectionUtils.isEmpty(getInsertColumns());
    }

    @Override
    public List<String> getInsertColumns() {
        List<SQLExpr> columnExprList = getColumnsExprList();
        if (CollectionUtils.isEmpty(columnExprList)) {
            return handleEmptyColumns(columnExprList);
        }
        return getInsertColumns(columnExprList);
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        List<SQLInsertStatement.ValuesClause> valuesClauses = getValuesClauses();
        if (CollectionUtils.isEmpty(valuesClauses)) {
            return handleEmptyValues(valuesClauses, primaryKeyIndex);
        }
        return getInsertRows(valuesClauses, primaryKeyIndex);
    }

    @Override
    public List<String> getInsertParamsValue() {
        List<SQLInsertStatement.ValuesClause> valuesClauses = getValuesClauses();
        if (CollectionUtils.isEmpty(valuesClauses)) {
            return Collections.emptyList();
        }
        List<String> valuesStrList = new ArrayList<>();
        for (SQLInsertStatement.ValuesClause clause : valuesClauses) {
            String values = clause.toString().replace("VALUES", "").trim();
            if (values.length() > 1) {
                values = values.substring(1, values.length() - 1);
            }
            valuesStrList.add(values);
        }
        return valuesStrList;
    }

    @Override
    public List<String> getDuplicateKeyUpdate() {
        return null;
    }


    protected List<String> getInsertColumns(List<SQLExpr> columnExprList) {
        List<String> insertColumns = new ArrayList<>(columnExprList.size());
        for (SQLExpr expr : columnExprList) {
            // support only identifier expression in inserted columns
            if (expr instanceof SQLIdentifierExpr) {
                insertColumns.add(((SQLIdentifierExpr) expr).getName());
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return insertColumns;
    }

    protected List<List<Object>> getInsertRows(List<SQLInsertStatement.ValuesClause> valuesClauses,
                                               Collection<Integer> primaryKeyIndex) {
        List<List<Object>> rows = new ArrayList<>(valuesClauses.size());
        for (SQLInsertStatement.ValuesClause valuesClause : valuesClauses) {
            List<SQLExpr> exprList = valuesClause.getValues();
            List<Object> row = new ArrayList<>(exprList.size());
            rows.add(row);
            for (int i = 0; i < exprList.size(); i++) {
                SQLExpr expr = exprList.get(i);
                // e.g. (null, 1, ?, sysdate(), default, seq.nextval)
                if (expr instanceof SQLNullExpr) {
                    row.add(Null.get());
                } else if (expr instanceof SQLValuableExpr) {
                    row.add(((SQLValuableExpr) expr).getValue());
                } else if (expr instanceof SQLVariantRefExpr) {
                    row.add(((SQLVariantRefExpr) expr).getName());
                } else if (expr instanceof SQLMethodInvokeExpr) {
                    row.add(SqlMethodExpr.get());
                } else if (expr instanceof SQLDefaultExpr) {
                    row.add(SqlDefaultExpr.get());
                } else if (expr instanceof SQLSequenceExpr) {
                    SQLSequenceExpr sequenceExpr = (SQLSequenceExpr) expr;
                    String sequence = sequenceExpr.getSequence().getSimpleName();
                    String function = sequenceExpr.getFunction().name;
                    row.add(new SqlSequenceExpr(sequence, function));
                } else {
                    if (primaryKeyIndex.contains(i)) {
                        wrapSQLParsingException(expr);
                    }
                    row.add(NotPlaceholderExpr.get());
                }
            }
        }
        return rows;
    }

    protected abstract List<SQLExpr> getColumnsExprList();

    protected abstract List<String> handleEmptyColumns(List<SQLExpr> columnExprList);

    protected abstract List<SQLInsertStatement.ValuesClause> getValuesClauses();

    protected abstract List<List<Object>> handleEmptyValues(List<SQLInsertStatement.ValuesClause> valuesClauses,
                                                            Collection<Integer> primaryKeyIndex);

    protected void getInsertSelectColumns(SQLSelectQuery selectQuery, final List<String> columns) {
        if (selectQuery instanceof SQLUnionQuery) {
            // 1. if selectQuery is a union-query
            SQLUnionQuery unionQuery = (SQLUnionQuery) selectQuery;
            // 1.1 get select list from left select-query block
            if (unionQuery.getLeft() instanceof SQLSelectQueryBlock) {
                List<SQLSelectItem> selectItems = ((SQLSelectQueryBlock) (unionQuery.getLeft())).getSelectList();
                getColumnNames(selectItems, columns);
            } else {
                throw new SQLParsingException("Unrecognizable left select query in union query: " + selectQuery);
            }
            // 1.2 get select list from right select-query block or union-query
            if (unionQuery.getRight() instanceof SQLSelectQueryBlock) {
                List<SQLSelectItem> selectItems = ((SQLSelectQueryBlock) (unionQuery.getRight())).getSelectList();
                getColumnNames(selectItems, columns);
            } else if (unionQuery.getRight() instanceof SQLUnionQuery) {
                getInsertSelectColumns(unionQuery.getRight(), columns);
            } else {
                throw new SQLParsingException("Unrecognizable right select query in union query: " + selectQuery);
            }
        } else if (selectQuery instanceof SQLSelectQueryBlock) {
            // 2. else if selectQuery is a select-query block
            SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) selectQuery;
            if (queryBlock.getFrom() instanceof SQLSubqueryTableSource) {
                // 2.1 if the table source in from clause is a sub-query, get select list from the sub-query
                getInsertSelectColumns(((SQLSubqueryTableSource) (queryBlock.getFrom())).getSelect().getQuery(), columns);
            } else {
                // 2.2 else, get select list by default
                List<SQLSelectItem> selectItems = ((SQLSelectQueryBlock) selectQuery).getSelectList();
                getColumnNames(selectItems, columns);
            }
        } else {
            throw new SQLParsingException("Unrecognizable select query: " + selectQuery);
        }
    }

    protected void getColumnNames(List<SQLSelectItem> selectItems, final List<String> columns) {
        for (SQLSelectItem columnClause : selectItems) {
            SQLExpr expr = columnClause.getExpr();
            if (expr instanceof SQLIdentifierExpr) {
                columns.add(((SQLIdentifierExpr) expr).getName());
            } else if (expr instanceof SQLPropertyExpr) {
                // table source case, e.g. select schema.table.column from ...
                columns.add(((SQLPropertyExpr) expr).getName());
            } else {
                wrapSQLParsingException(expr);
            }
        }
    }
}
