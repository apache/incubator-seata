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
package io.seata.sqlparser.antlr.mysql.visit;

import io.seata.sqlparser.antlr.mysql.MySqlContext;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParserBaseVisitor;

import java.util.Arrays;
import java.util.List;

/**
 * InsertSpecificationSqlVisitor
 *
 * @author zhihou
 */
public class InsertSpecificationSqlVisitor extends MySqlParserBaseVisitor<MySqlContext> {

    private MySqlContext mySqlContext;

    public InsertSpecificationSqlVisitor(MySqlContext mySqlContext) {
        this.mySqlContext = mySqlContext;
    }

    @Override
    public MySqlContext visitInsertStatement(MySqlParser.InsertStatementContext ctx) {

        MySqlParser.TableNameContext tableNameContext = ctx.tableName();

        mySqlContext.setTableName(tableNameContext.getText());

        MySqlParser.UidListContext columns = ctx.columns;

        List<String> strings = Arrays.asList(columns.getText().split(","));

        for (String insertColumnName : strings) {
            mySqlContext.addForInsertColumnName(insertColumnName);
        }

        MySqlParser.InsertStatementValueContext insertStatementValueContext = ctx.insertStatementValue();
        List<MySqlParser.ExpressionsWithDefaultsContext> expressionsWithDefaultsContexts = insertStatementValueContext.expressionsWithDefaults();

        for (MySqlParser.ExpressionsWithDefaultsContext expressions : expressionsWithDefaultsContexts) {

            String text = expressions.getText();
            String str = null;
            if (text.contains("'")) {
                str = text.replace("'", "");
            } else if (text.contains("\"")) {
                str = text.replace("\"", "");
            } else {
                str = text;
            }
            if (!str.isEmpty() && !str.contains("'") && !str.contains("\"")) {
                mySqlContext.addForInsertValColumnName(Arrays.asList(str.split(",")));
            }
        }
        mySqlContext.setInsertRows(expressionsWithDefaultsContexts.size());
        return this.mySqlContext;
    }
}
