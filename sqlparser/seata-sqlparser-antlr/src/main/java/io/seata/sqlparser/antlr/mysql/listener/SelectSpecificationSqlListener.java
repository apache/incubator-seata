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

import java.util.List;

/**
 * @author houzhi
 */
public class SelectSpecificationSqlListener extends MySqlParserBaseListener {

    private MySqlContext sqlQueryContext;

    public SelectSpecificationSqlListener(MySqlContext sqlQueryContext) {
        this.sqlQueryContext = sqlQueryContext;
    }

    @Override
    public void enterTableName(MySqlParser.TableNameContext ctx) {

        sqlQueryContext.setTableName(ctx.getText());
        super.enterTableName(ctx);
    }

    @Override
    public void enterAtomTableItem(MySqlParser.AtomTableItemContext ctx) {

        MySqlParser.UidContext uid = ctx.uid();
        if (uid != null) {
            String text = uid.getText();
            if (!text.isEmpty()) {
                sqlQueryContext.setTableAlias(text);
            }
        }
        super.enterAtomTableItem(ctx);
    }

    @Override
    public void enterFromClause(MySqlParser.FromClauseContext ctx) {

        MySqlParser.ExpressionContext whereExpr = ctx.whereExpr;
        StatementSqlVisitor statementSqlVisitor = new StatementSqlVisitor();
        String text = statementSqlVisitor.visit(whereExpr).toString();
        sqlQueryContext.setWhereCondition(text);
        super.enterFromClause(ctx);
    }

    @Override
    public void enterFullColumnNameExpressionAtom(MySqlParser.FullColumnNameExpressionAtomContext ctx) {

        sqlQueryContext.addQueryWhereColumnNames(ctx.getText());
        super.enterFullColumnNameExpressionAtom(ctx);
    }

    @Override
    public void enterConstantExpressionAtom(MySqlParser.ConstantExpressionAtomContext ctx) {

        sqlQueryContext.addQueryWhereValColumnNames(ctx.getText());
        super.enterConstantExpressionAtom(ctx);
    }

    @Override
    public void enterSelectElements(MySqlParser.SelectElementsContext ctx) {

        List<MySqlParser.SelectElementContext> selectElementContexts = ctx.selectElement();
        for (MySqlParser.SelectElementContext selectElementContext : selectElementContexts) {
            sqlQueryContext.addQueryColumnNames(selectElementContext.getText());
        }
        super.enterSelectElements(ctx);
    }
}
