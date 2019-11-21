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
package io.seata.rm.datasource.sql.druid;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;

import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.SQLParsingException;
import io.seata.rm.datasource.sql.SQLType;
import io.seata.rm.datasource.sql.struct.Null;
import io.seata.rm.datasource.sql.struct.SqlMethodExpr;

/**
 * The type My sql insert recognizer.
 *
 * @author sharajava
 */
public class MySQLInsertRecognizer extends BaseMySQLRecognizer implements SQLInsertRecognizer {

    private final MySqlInsertStatement ast;

    /**
     * Instantiates a new My sql insert recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public MySQLInsertRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (MySqlInsertStatement)ast;
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
        MySqlOutputVisitor visitor = new MySqlOutputVisitor(sb) {

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
    public List<String> getInsertColumns() {
        List<SQLExpr> columnSQLExprs = ast.getColumns();
        if (columnSQLExprs.size() == 0) {
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
    public List<List<Object>> getInsertRows() {
        List<SQLInsertStatement.ValuesClause> valuesClauses = ast.getValuesList();
        List<List<Object>> rows = new ArrayList<>(valuesClauses.size());
        for (SQLInsertStatement.ValuesClause valuesClause : valuesClauses) {
            List<SQLExpr> exprs = valuesClause.getValues();
            List<Object> row = new ArrayList<>(exprs.size());
            rows.add(row);
            for (SQLExpr expr : valuesClause.getValues()) {
                if (expr instanceof SQLNullExpr) {
                    row.add(Null.get());
                } else if (expr instanceof SQLValuableExpr) {
                    row.add(((SQLValuableExpr)expr).getValue());
                } else if (expr instanceof SQLVariantRefExpr) {
                    row.add(((SQLVariantRefExpr)expr).getName());
                } else if (expr instanceof SQLMethodInvokeExpr) {
                    row.add(new SqlMethodExpr());
                } else {
                    throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
                }
            }
        }
        return rows;
    }
}
