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
package io.seata.sqlparser.antlr.oracle.listener;

import io.seata.sqlparser.antlr.oracle.OracleContext;
import io.seata.sqlparser.antlr.oracle.parser.OracleBaseListener;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.visitor.StatementSqlVisitor;

import java.util.List;

/**
 * @author YechenGu
 */
public class UpdateSpecificationSqlListener extends OracleBaseListener {
    private OracleContext oracleContext;

    public UpdateSpecificationSqlListener(OracleContext oracleContext) {
        this.oracleContext = oracleContext;
    }

    @Override
    public void enterDml_table_expression_clause(OracleParser.Dml_table_expression_clauseContext ctx){
        oracleContext.setTableName(ctx.getText());
        super.enterDml_table_expression_clause(ctx);
    }

    @Override
    public void enterGeneral_table_ref(OracleParser.General_table_refContext ctx){
        OracleParser.Table_aliasContext table_aliasContext = ctx.table_alias();
        if (table_aliasContext != null){
            String text = table_aliasContext.getText();
            oracleContext.setTableAlias(text);
        }
        super.enterGeneral_table_ref(ctx);
    }

    @Override
    public void enterWhere_clause(OracleParser.Where_clauseContext ctx){
        OracleParser.ExpressionContext expressionContext = ctx.expression();
        StatementSqlVisitor visitor = new StatementSqlVisitor();
        String text = visitor.visit(expressionContext).toString();
        oracleContext.setWhereCondition(text);

        // Get each single where condition
        OracleParser.Logical_expressionContext logicalExpressionContext = expressionContext.logical_expression();

        // Judge whether it is a complicated where clause
        if (logicalExpressionContext.children.size() > 1){
            List<OracleParser.Logical_expressionContext> logical_expressionContexts = logicalExpressionContext.logical_expression();
            while (true){
                // Loop until all logical_expressions are analysed
                if (logical_expressionContexts.get(0).logical_expression().isEmpty()){
                    break;
                }
                OracleParser.Logical_expressionContext lec0 = logical_expressionContexts.get(0).logical_expression().get(0);
                OracleParser.Logical_expressionContext lec1 = logical_expressionContexts.get(0).logical_expression().get(1);
                logical_expressionContexts.remove(0);
                logical_expressionContexts.add(0,lec0);
                logical_expressionContexts.add(1,lec1);
            }

            for (OracleParser.Logical_expressionContext lec:logical_expressionContexts){
                StatementSqlVisitor lecVisitor = new StatementSqlVisitor();
                String lecText = lecVisitor.visit(lec).toString();
                oracleContext.addUpdateWhereColumn(lecText);
            }
        } else {
            StatementSqlVisitor lecVisitor = new StatementSqlVisitor();
            String lecText = lecVisitor.visit(logicalExpressionContext).toString();
            oracleContext.addUpdateWhereColumn(lecText);
        }

        super.enterWhere_clause(ctx);
    }

    @Override
    public void enterColumn_based_update_set_clause(OracleParser.Column_based_update_set_clauseContext ctx){
        StatementSqlVisitor visitor = new StatementSqlVisitor();
        String text = visitor.visit(ctx).toString();
        oracleContext.addUpdateColumn(text);
        super.enterColumn_based_update_set_clause(ctx);
    }
}
