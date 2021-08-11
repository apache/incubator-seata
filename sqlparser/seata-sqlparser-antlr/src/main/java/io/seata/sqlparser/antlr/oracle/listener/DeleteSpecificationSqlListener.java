package io.seata.sqlparser.antlr.oracle.listener;

import io.seata.sqlparser.antlr.oracle.OracleContext;
import io.seata.sqlparser.antlr.oracle.parser.OracleBaseListener;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.visitor.StatementSqlVisitor;

import java.util.List;

/**
 * @author YechenGu
 */
public class DeleteSpecificationSqlListener extends OracleBaseListener {
    private OracleContext oracleContext;

    public DeleteSpecificationSqlListener(OracleContext oracleContext) {
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
                oracleContext.addDeleteWhereColumn(lecText);
            }
        } else {
            StatementSqlVisitor lecVisitor = new StatementSqlVisitor();
            String lecText = lecVisitor.visit(logicalExpressionContext).toString();
            oracleContext.addDeleteWhereColumn(lecText);
        }

        super.enterWhere_clause(ctx);
    }


}
