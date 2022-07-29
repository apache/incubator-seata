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
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.common.exception.NotSupportYetException;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlDefaultExpr;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;

import java.util.ArrayList;
import java.util.List;

/**
 * Update recognizer for OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleUpdateRecognizer extends BaseOceanBaseOracleRecognizer implements SQLUpdateRecognizer {
    private final OracleUpdateStatement ast;

    public OceanBaseOracleUpdateRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (OracleUpdateStatement) ast;
    }

    @Override
    protected SQLStatement getAst() {
        return ast;
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
        OracleOutputVisitor visitor = new OracleOutputVisitor(sb) {

            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };

        SQLTableSource tableSource = ast.getTableSource();
        if (tableSource instanceof SQLExprTableSource) {
            visitor.visit((SQLExprTableSource) tableSource);
        } else {
            throw new NotSupportYetException("No support for syntax with the table reference: " +
                tableSource.getClass().getName());
        }
        return sb.toString();
    }

    @Override
    public List<String> getUpdateColumns() {
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<String> updateColumns = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            // setItem => set 'column'='value'
            SQLExpr expr = updateSetItem.getColumn();
            if (expr instanceof SQLIdentifierExpr) {
                updateColumns.add(((SQLIdentifierExpr) expr).getName());
            } else if (expr instanceof SQLPropertyExpr) {
                SQLPropertyExpr propertyExpr = (SQLPropertyExpr) expr;
                SQLExpr owner = propertyExpr.getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    // table alias case, like: update test t set t.id = 1 where ...
                    updateColumns.add(((SQLIdentifierExpr) owner).getName() + "." + propertyExpr.getName());
                } else if (propertyExpr.getOwnerName().contains(".")) {
                    // full table source case, like: update d.t set d.t.id = 1 where ...
                    updateColumns.add(propertyExpr.getOwnerName() + "." + propertyExpr.getName());
                }
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return updateColumns;
    }

    @Override
    public List<Object> getUpdateValues() {
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<Object> updateValues = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            SQLExpr expr = updateSetItem.getValue();
            if (expr instanceof SQLNullExpr) {
                updateValues.add(Null.get());
            } else if (expr instanceof SQLValuableExpr) {
                updateValues.add(((SQLValuableExpr) expr).getValue());
            } else if (expr instanceof SQLVariantRefExpr) {
                updateValues.add(((SQLVariantRefExpr) expr).getName());
            } else if (expr instanceof SQLMethodInvokeExpr) {
                updateValues.add(SqlMethodExpr.get());
            } else if (expr instanceof SQLDefaultExpr) {
                updateValues.add(SqlDefaultExpr.get());
            } else if (expr instanceof SQLSequenceExpr) {
                SQLSequenceExpr sequenceExpr = (SQLSequenceExpr) expr;
                String sequence = sequenceExpr.getSequence().getSimpleName();
                String function = sequenceExpr.getFunction().name;
                updateValues.add(new SqlSequenceExpr(sequence, function));
            } else {
                wrapSQLParsingException(expr);
            }
        }
        return updateValues;
    }

    @Override
    public String getWhereCondition() {
        SQLExpr where = ast.getWhere();
        return super.getWhereCondition(where);
    }

    @Override
    public String getWhereCondition(final ParametersHolder parametersHolder,
                                    final ArrayList<List<Object>> paramAppenderList) {
        SQLExpr where = ast.getWhere();
        return super.getWhereCondition(where, parametersHolder, paramAppenderList);
    }

    @Override
    public String getLimitCondition() {
        // oracle does not support limit
        return null;
    }

    @Override
    public String getLimitCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        // oracle does not support limit
        return null;
    }

    @Override
    public String getOrderByCondition() {
        // oracle does not support order by yet
        return null;
    }

    @Override
    public String getOrderByCondition(ParametersHolder parametersHolder, ArrayList<List<Object>> paramAppenderList) {
        // oracle does not support order by yet
        return null;
    }
}
