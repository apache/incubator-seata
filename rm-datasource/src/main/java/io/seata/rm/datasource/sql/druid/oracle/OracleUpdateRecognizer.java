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
package io.seata.rm.datasource.sql.druid.oracle;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.ast.expr.SQLVariantRefExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLBetweenExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.visitor.OracleOutputVisitor;
import io.seata.rm.datasource.ParametersHolder;
import io.seata.rm.datasource.sql.SQLParsingException;
import io.seata.rm.datasource.sql.SQLType;
import io.seata.rm.datasource.sql.SQLUpdateRecognizer;
import io.seata.rm.datasource.sql.druid.BaseRecognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * The type oracle update recognizer.
 *
 * @author ccg
 * @date 2019/3/25
 */
public class OracleUpdateRecognizer extends BaseRecognizer implements SQLUpdateRecognizer {

    private OracleUpdateStatement ast;

    /**
     * Instantiates a new My sql update recognizer.
     *
     * @param originalSQL the original sql
     * @param ast         the ast
     */
    public OracleUpdateRecognizer(String originalSQL, SQLStatement ast) {
        super(originalSQL);
        this.ast = (OracleUpdateStatement) ast;
    }

    @Override
    public SQLType getSQLType() {
        return SQLType.UPDATE;
    }

    @Override
    public List<String> getUpdateColumns() {
        List<SQLUpdateSetItem> updateSetItems = ast.getItems();
        List<String> list = new ArrayList<>(updateSetItems.size());
        for (SQLUpdateSetItem updateSetItem : updateSetItems) {
            SQLExpr expr = updateSetItem.getColumn();
            if (expr instanceof SQLIdentifierExpr) {
                list.add(((SQLIdentifierExpr) expr).getName().toUpperCase());
            } else if (expr instanceof SQLPropertyExpr) {
                // This is alias case, like UPDATE xxx_tbl a SET a.name = ? WHERE a.id = ?
                SQLExpr owner = ((SQLPropertyExpr) expr).getOwner();
                if (owner instanceof SQLIdentifierExpr) {
                    list.add((((SQLIdentifierExpr) owner).getName() + "." + ((SQLPropertyExpr) expr).getName()));
                }
            } else {
                throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
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
                list.add(((SQLValuableExpr) expr).getValue());
            } else if (expr instanceof SQLVariantRefExpr) {
                list.add(new VMarker());
            } else {
                throw new SQLParsingException("Unknown SQLExpr: " + expr.getClass() + " " + expr);
            }
        }
        return list;
    }

    @Override
    public String getWhereCondition(final ParametersHolder parametersHolder, final ArrayList<List<Object>> paramAppenderList) {
        SQLExpr where = ast.getWhere();
        if (where == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        OracleOutputVisitor visitor = super.createOracleOutputVisitor(parametersHolder, paramAppenderList, sb);
        visitor.visit((SQLBinaryOpExpr) where);
        return sb.toString();
    }

    @Override
    public String getWhereCondition() {
        SQLExpr where = ast.getWhere();
        if (where == null) {
            return "";
        }


        StringBuffer sb = new StringBuffer();
        OracleOutputVisitor visitor = new OracleOutputVisitor(sb);

        if (where instanceof SQLBetweenExpr) {
            visitor.visit((SQLBetweenExpr) where);
        } else if (where instanceof SQLInListExpr) {
            visitor.visit((SQLInListExpr) where);
        } else {
            visitor.visit((SQLBinaryOpExpr) where);
        }

        return sb.toString();
    }

    @Override
    public String getTableAlias() {
        return ast.getTableSource().getAlias();
    }

    @Override
    public String getTableName() {
        StringBuffer sb = new StringBuffer();
        OracleOutputVisitor visitor = new OracleOutputVisitor(sb) {

            @Override
            public boolean visit(SQLExprTableSource x) {
                printTableSourceExpr(x.getExpr());
                return false;
            }
        };
        SQLExprTableSource tableSource = (SQLExprTableSource) ast.getTableSource();
        visitor.visit(tableSource);
        return sb.toString();
    }

}
