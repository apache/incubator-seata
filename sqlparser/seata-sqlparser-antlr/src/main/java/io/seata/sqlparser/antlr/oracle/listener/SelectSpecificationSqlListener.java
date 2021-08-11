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
public class SelectSpecificationSqlListener extends OracleBaseListener {

    private OracleContext oracleContext;

    public SelectSpecificationSqlListener(OracleContext oracleContext) {
        this.oracleContext = oracleContext;
    }

    @Override
    public void enterSelected_list(OracleParser.Selected_listContext ctx){
        List<OracleParser.Select_list_elementsContext> contexts = ctx.select_list_elements();
        if(contexts.size() == 0){
            oracleContext.addQueryColumnNames("*");
        }else{
            for (OracleParser.Select_list_elementsContext context:contexts){
                oracleContext.addQueryColumnNames(context.getText());
            }
        }
        super.enterSelected_list(ctx);
    }

    @Override
    public void enterDml_table_expression_clause(OracleParser.Dml_table_expression_clauseContext ctx){
        oracleContext.setTableName(ctx.getText());
        super.enterDml_table_expression_clause(ctx);
    }

    @Override
    public void enterTable_ref_aux(OracleParser.Table_ref_auxContext ctx){
        OracleParser.Table_aliasContext table_aliasContext = ctx.table_alias();
        if (table_aliasContext != null){
            String text = table_aliasContext.getText();
            oracleContext.setTableAlias(text);
        }
        super.enterTable_ref_aux(ctx);
    }

    @Override
    public void enterWhere_clause(OracleParser.Where_clauseContext ctx){
        OracleParser.ExpressionContext expressionContext = ctx.expression();
        StatementSqlVisitor visitor = new StatementSqlVisitor();
        String text = visitor.visit(expressionContext).toString();
        oracleContext.setWhereCondition(text);

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
                oracleContext.addQueryWhereColumn(lecText);
            }
        } else {
            StatementSqlVisitor lecVisitor = new StatementSqlVisitor();
            String lecText = lecVisitor.visit(logicalExpressionContext).toString();
            oracleContext.addQueryWhereColumn(lecText);
        }

        super.enterWhere_clause(ctx);
    }

}
