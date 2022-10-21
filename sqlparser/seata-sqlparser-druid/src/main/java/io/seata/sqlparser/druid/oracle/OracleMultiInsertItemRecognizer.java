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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.oracle.ast.OracleSQLObject;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLType;

/**
 * The type oracle multi insert recognizer,just like "insert all into table1 into table2 select * from table3"
 *
 * @author renliangyu857
 */
public class OracleMultiInsertItemRecognizer extends BaseOracleRecognizer implements SQLInsertRecognizer {

    private final OracleMultiInsertStatement ast;

    private final OracleSQLObject item;

    public OracleMultiInsertItemRecognizer(String originalSQL, SQLStatement ast, OracleSQLObject item) {
        super(originalSQL);
        this.ast = (OracleMultiInsertStatement) ast;
        this.item = item;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.INSERT;
    }

    @Override
    public String getTableAlias() {
        if (this.item instanceof OracleMultiInsertStatement.InsertIntoClause) {
            return ((OracleMultiInsertStatement.InsertIntoClause) this.item).getAlias();
        } else {
            throw new NotSupportYetException("not support the batch insert sql syntax with not a InsertIntoClause");
        }
    }

    @Override
    public String getTableName() {
        StringBuilder tableName = new StringBuilder();
        if (this.item instanceof OracleMultiInsertStatement.InsertIntoClause) {
            visitTableName(((OracleMultiInsertStatement.InsertIntoClause) this.item).getTableSource(), tableName);
        } else {
            throw new NotSupportYetException("not support the batch insert sql syntax with not a InsertIntoClause");
        }
        return tableName.toString();
    }

    @Override
    public boolean insertColumnsIsEmpty() {
        return CollectionUtils.isEmpty(this.getInsertColumns());
    }

    @Override
    public List<String> getInsertColumns() {
        OracleMultiInsertStatement.InsertIntoClause insertIntoItem;
        if (this.item instanceof OracleMultiInsertStatement.InsertIntoClause) {
            insertIntoItem = (OracleMultiInsertStatement.InsertIntoClause) this.item;
        } else {
            throw new NotSupportYetException("not support the batch insert sql syntax with not a InsertIntoClause");
        }
        List<SQLExpr> columnSQLExprs = insertIntoItem.getColumns();
        List<String> columns = new ArrayList<>();
        if (columnSQLExprs.isEmpty()) {
            //just like "insert into table1 select * from table2"
            parseInsertSelectColumns(ast.getSubQuery().getQuery(),columns);
            return columns;
        }
        for (SQLExpr expr : columnSQLExprs) {
            if (expr instanceof SQLIdentifierExpr) {
                columns.add(((SQLIdentifierExpr)expr).getName());
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return columns;
    }

    @Override
    public List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        //get rows when executing query sql
        return Collections.emptyList();
    }

    @Override
    public List<String> getInsertParamsValue() {
        //only support for "insert all into table1 select * from table2"
        return Collections.emptyList();
    }

    @Override
    public List<String> getDuplicateKeyUpdate() {
        return Collections.emptyList();
    }

    @Override
    protected SQLStatement getAst() {
        return this.ast;
    }

    private void visitTableName(SQLExprTableSource tableSource, StringBuilder tableName) {
        OracleOutputVisitor visitor = new OracleOutputVisitor(tableName) {

            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        visitor.visit(tableSource);
    }

    @Override
    public String getSubQuerySql() {
        if (this.item instanceof OracleMultiInsertStatement.InsertIntoClause) {
            return (this.ast.getSubQuery() != null && this.ast.getSubQuery().getQuery() != null) ? this.ast.getSubQuery().getQuery().toString() : "";
        } else {
            throw new NotSupportYetException("not support the batch insert sql syntax with not a InsertIntoClause");
        }
    }
}
