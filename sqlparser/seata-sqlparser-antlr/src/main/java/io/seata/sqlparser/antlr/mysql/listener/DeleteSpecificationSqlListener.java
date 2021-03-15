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
package io.seata.sqlparser.antlr.mysql.listener;

import io.seata.sqlparser.antlr.mysql.MySqlContext;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParserBaseListener;
import io.seata.sqlparser.antlr.mysql.visit.StatementSqlVisitor;

/**
 * @author houzhi
 */
public class DeleteSpecificationSqlListener extends MySqlParserBaseListener {

    private MySqlContext sqlQueryContext;

    public DeleteSpecificationSqlListener(MySqlContext sqlQueryContext) {
        this.sqlQueryContext = sqlQueryContext;
    }

    @Override
    public void enterAtomTableItem(MySqlParser.AtomTableItemContext ctx) {
        MySqlParser.TableNameContext tableNameContext = ctx.tableName();
        sqlQueryContext.setTableName(tableNameContext.getText());
        MySqlParser.UidContext uid = ctx.uid();
        sqlQueryContext.setTableAlias(uid.getText());
        super.enterAtomTableItem(ctx);
    }

    @Override
    public void enterConstantExpressionAtom(MySqlParser.ConstantExpressionAtomContext ctx) {
        sqlQueryContext.addDeleteWhereValColumnNames(ctx.getText());
        super.enterConstantExpressionAtom(ctx);
    }

    @Override
    public void enterFullColumnNameExpressionAtom(MySqlParser.FullColumnNameExpressionAtomContext ctx) {
        sqlQueryContext.addDeleteWhereColumnNames(ctx.getText());
        super.enterFullColumnNameExpressionAtom(ctx);
    }

    @Override
    public void enterSingleDeleteStatement(MySqlParser.SingleDeleteStatementContext ctx) {
        MySqlParser.TableNameContext tableNameContext = ctx.tableName();
        sqlQueryContext.setTableName(tableNameContext.getText());
        MySqlParser.ExpressionContext expression = ctx.expression();
        StatementSqlVisitor statementSqlVisitor = new StatementSqlVisitor();
        String text = statementSqlVisitor.visit(expression).toString();
        sqlQueryContext.setWhereCondition(text);
        super.enterSingleDeleteStatement(ctx);
    }

    @Override
    public void enterMultipleDeleteStatement(MySqlParser.MultipleDeleteStatementContext ctx) {
        MySqlParser.ExpressionContext expression = ctx.expression();
        StatementSqlVisitor statementSqlVisitor = new StatementSqlVisitor();
        String text = statementSqlVisitor.visit(expression).toString();
        sqlQueryContext.setWhereCondition(text);
        super.enterMultipleDeleteStatement(ctx);
    }

    @Override
    public void enterInPredicate(MySqlParser.InPredicateContext ctx) {
        StatementSqlVisitor statementSqlVisitor = new StatementSqlVisitor();
        String text = statementSqlVisitor.visit(ctx).toString();
        sqlQueryContext.setWhereCondition(text);
        super.enterInPredicate(ctx);
    }

}