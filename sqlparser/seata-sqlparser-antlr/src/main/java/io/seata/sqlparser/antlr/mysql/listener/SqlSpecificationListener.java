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

import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.antlr.mysql.MySqlContext;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParserBaseListener;

import java.util.List;

/**
 * @author houzhi
 */
public class SqlSpecificationListener extends MySqlParserBaseListener {

    private MySqlContext sqlQueryContext;

    public SqlSpecificationListener(MySqlContext sqlQueryContext) {
        this.sqlQueryContext = sqlQueryContext;
    }

    @Override
    public void enterSqlStatements(MySqlParser.SqlStatementsContext ctx) {

        List<MySqlParser.SqlStatementContext> sqlStatementContexts = ctx.sqlStatement();

        MySqlContext.SQL sql = null;

        for (MySqlParser.SqlStatementContext context : sqlStatementContexts) {

            sql = new MySqlContext.SQL();
            MySqlParser.DmlStatementContext dmlStatementContext = context.dmlStatement();

            MySqlParser.UpdateStatementContext updateStatementContext = dmlStatementContext.updateStatement();
            if (updateStatementContext != null) {
                sql.setSqlType(SQLType.UPDATE.value());
            }
            MySqlParser.InsertStatementContext insertStatementContext = dmlStatementContext.insertStatement();
            if (insertStatementContext != null) {
                sql.setSqlType(SQLType.INSERT.value());
            }
            MySqlParser.DeleteStatementContext deleteStatementContext = dmlStatementContext.deleteStatement();
            if (deleteStatementContext != null) {
                sql.setSqlType(SQLType.DELETE.value());
            }
            MySqlParser.SelectStatementContext selectStatementContext = dmlStatementContext.selectStatement();
            if (selectStatementContext != null) {
                sql.setSqlType(SQLType.SELECT.value());
            }
            sql.setSql(context.getText());
            sqlQueryContext.addSqlInfo(sql);
        }
        super.enterSqlStatements(ctx);
    }
}
