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
package io.seata.sqlparser.druid.oracle;

import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectQueryBlock;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectSubqueryTableSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLSequenceExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.NotPlaceholderExpr;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;

/**
 * The type oracle insert recognizer.
 *
 * @author ccg
 */
public class OracleInsertRecognizer extends BaseOracleRecognizer implements SQLInsertRecognizer {

    private final OracleInsertStatement ast;

    /**
     * Instantiates a new My sql insert recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public OracleInsertRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (OracleInsertStatement)ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
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
        };
        visitor.visit(ast.getTableSource());
        return sb.toString();
    }

    @Override
    public boolean insertColumnsIsEmpty() {
        return CollectionUtils.isEmpty(ast.getColumns());
    }

    @Override
    public List<String> getInsertColumns() {
        List<SQLExpr> columnSQLExprs = ast.getColumns();
        if (columnSQLExprs.isEmpty()) {
            // INSERT INTO ta VALUES (...), without fields clarified
            return null;
        }
        List<String> list = new ArrayList<>(columnSQLExprs.size());
        for (SQLExpr expr : columnSQLExprs) {
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr)expr).getName());
            } else {
                throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
            }
        }
        return list;
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        List<SQLInsertStatement.ValuesClause> valuesClauses = ast.getValuesList();
        //1.insert into table1 select * from dual uniou select * from dual
        //2.insert into table1 select * from (select * from dual uniou select * from dual)
        if(valuesClauses.isEmpty() && ast.getQuery() != null) {
            List<List<Object>> rows = new ArrayList<>();
            this.getInsertSelectRows(ast.getQuery().getQuery(),rows,primaryKeyIndex);
            return rows;
        }
        List<List<Object>> rows = new ArrayList<>(valuesClauses.size());
        for (SQLInsertStatement.ValuesClause valuesClause : valuesClauses) {
            List<SQLExpr> exprs = valuesClause.getValues();
            List<Object> row = new ArrayList<>(exprs.size());
            rows.add(row);
            for (int i = 0, len = exprs.size(); i < len; i++) {
                SQLExpr expr = exprs.get(i);
                if (expr instanceof SQLNullExpr) {
                    row.add(Null.get());
                } else if (expr instanceof SQLValuableExpr) {
                    row.add(((SQLValuableExpr) expr).getValue());
                } else if (expr instanceof SQLVariantRefExpr) {
                    row.add(((SQLVariantRefExpr) expr).getName());
                } else if (expr instanceof SQLMethodInvokeExpr) {
                    row.add(SqlMethodExpr.get());
                } else if (expr instanceof SQLSequenceExpr) {
                    SQLSequenceExpr sequenceExpr = (SQLSequenceExpr) expr;
                    String sequence = sequenceExpr.getSequence().getSimpleName();
                    String function = sequenceExpr.getFunction().name;
                    row.add(new SqlSequenceExpr(sequence, function));
                } else {
                    if (primaryKeyIndex.contains(i)) {
                        throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
                    }
                    row.add(NotPlaceholderExpr.get());
                }
            }
        }
        return rows;
    }

    private void getInsertSelectRows(SQLSelectQuery selectQuery,List<List<Object>> rows ,Collection<Integer> primaryKeyIndex) {
        if(selectQuery instanceof  SQLUnionQuery) {
            //a: get left(SQLSelectQueryBlock)
            List<SQLSelectItem> selectItems = ((SQLSelectQueryBlock) ((SQLUnionQuery)selectQuery).getLeft()).getSelectList();
            this.converSQLSelectItems(selectItems,rows,primaryKeyIndex);
            //b:  get right(SQLUnionQuery)
            if(((SQLUnionQuery)selectQuery).getRight() instanceof SQLUnionQuery) {
               this.getInsertSelectRows(((SQLUnionQuery)selectQuery).getRight(),rows,primaryKeyIndex);
            }
            //b:  get right(SQLSelectQueryBlock)
            else {
                selectItems = ((SQLSelectQueryBlock)((SQLUnionQuery)selectQuery).getRight()).getSelectList();
                this.converSQLSelectItems(selectItems,rows,primaryKeyIndex);
            }
        }
        //SQLSelectQueryBlock
        else {
            //select * from (select * from dual union select * from dual)
            if(((SQLSelectQueryBlock) selectQuery).getFrom() instanceof OracleSelectSubqueryTableSource) {
                this.getInsertSelectRows(((OracleSelectSubqueryTableSource)((SQLSelectQueryBlock) selectQuery).getFrom()).getSelect().getQuery(),rows,primaryKeyIndex);
            }
            //select * from dual union select * from dual
            else {
                List<SQLSelectItem> selectItems = ((SQLSelectQueryBlock) selectQuery).getSelectList();
                this.converSQLSelectItems(selectItems, rows, primaryKeyIndex);
            }
        }
    }

    private void converSQLSelectItems(List<SQLSelectItem> sqlSelectItem,List<List<Object>> rows ,Collection<Integer> primaryKeyIndex) {
        List<Object> row = new ArrayList<>();
        rows.add(row);
        for (SQLSelectItem valuesClause : sqlSelectItem) {
            SQLExpr expr = valuesClause.getExpr();
            if (expr instanceof SQLNullExpr) {
                row.add(Null.get());
            } else if (expr instanceof SQLValuableExpr) {
                row.add(((SQLValuableExpr) expr).getValue());
            } else if (expr instanceof SQLVariantRefExpr) {
                row.add(((SQLVariantRefExpr) expr).getName());
            } else if (expr instanceof SQLMethodInvokeExpr) {
                row.add(SqlMethodExpr.get());
            } else if (expr instanceof SQLSequenceExpr) {
                SQLSequenceExpr sequenceExpr = (SQLSequenceExpr) expr;
                String sequence = sequenceExpr.getSequence().getSimpleName();
                String function = sequenceExpr.getFunction().name;
                row.add(new SqlSequenceExpr(sequence, function));
            } else {
//                if (primaryKeyIndex.contains(i)) {
//                    throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
//                }
                row.add(NotPlaceholderExpr.get());
            }
        }

    }
    public OracleInsertStatement getAst() {
        return ast;
    }

}
