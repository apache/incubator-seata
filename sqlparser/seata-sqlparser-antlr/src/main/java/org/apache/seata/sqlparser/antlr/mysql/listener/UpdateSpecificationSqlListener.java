/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.sqlparser.antlr.mysql.listener;

import org.apache.seata.sqlparser.antlr.mysql.MySqlContext;
import org.apache.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import org.apache.seata.sqlparser.antlr.mysql.parser.MySqlParserBaseListener;
import org.apache.seata.sqlparser.antlr.mysql.visit.StatementSqlVisitor;


public class UpdateSpecificationSqlListener extends MySqlParserBaseListener {


    private MySqlContext sqlQueryContext;

    public UpdateSpecificationSqlListener(MySqlContext sqlQueryContext) {
        this.sqlQueryContext = sqlQueryContext;
    }

    @Override
    public void enterTableName(MySqlParser.TableNameContext ctx) {

        sqlQueryContext.setTableName(ctx.getText());
        super.enterTableName(ctx);
    }

    @Override
    public void enterConstantExpressionAtomForUpdate(MySqlParser.ConstantExpressionAtomForUpdateContext ctx) {

        sqlQueryContext.addUpdateWhereValColumnNames(ctx.getText());
        super.enterConstantExpressionAtomForUpdate(ctx);
    }

    @Override
    public void enterFullColumnNameExpressionAtomForUpdate(MySqlParser.FullColumnNameExpressionAtomForUpdateContext ctx) {

        sqlQueryContext.addUpdateWhereColumnNames(ctx.getText());
        super.enterFullColumnNameExpressionAtomForUpdate(ctx);
    }

    @Override
    public void enterSingleUpdateStatement(MySqlParser.SingleUpdateStatementContext ctx) {

        MySqlParser.ExpressionForUpdateContext expressionForUpdateContext = ctx.expressionForUpdate();
        StatementSqlVisitor statementSqlVisitor = new StatementSqlVisitor();
        String text = statementSqlVisitor.visit(expressionForUpdateContext).toString();
        sqlQueryContext.setWhereCondition(text);

        MySqlParser.UidContext uid = ctx.uid();

        if (uid != null) {
            String alias = uid.getText();
            if (!text.isEmpty()) {
                sqlQueryContext.setTableAlias(alias);
            }
        }
        super.enterSingleUpdateStatement(ctx);
    }

    @Override
    public void enterUpdatedElement(MySqlParser.UpdatedElementContext ctx) {

        MySqlParser.ExpressionContext expression = ctx.expression();
        sqlQueryContext.addUpdateValues(expression.getText());

        MySqlParser.FullColumnNameContext fullColumnNameContext = ctx.fullColumnName();
        sqlQueryContext.addUpdateColumnNames(fullColumnNameContext.getText());

        super.enterUpdatedElement(ctx);
    }

}
