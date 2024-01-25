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
package org.apache.seata.sqlparser.antlr.mysql;

import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.antlr.SQLOperateRecognizerHolder;
import org.apache.seata.sqlparser.antlr.SQLOperateRecognizerHolderFactory;
import org.apache.seata.sqlparser.antlr.mysql.parser.MySqlLexer;
import org.apache.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import org.apache.seata.sqlparser.antlr.mysql.stream.ANTLRNoCaseStringStream;
import org.apache.seata.sqlparser.antlr.mysql.visit.StatementSqlVisitor;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;

/**
 * AntlrMySQLRecognizerFactory
 *
 */
class AntlrMySQLRecognizerFactory implements SQLRecognizerFactory {

    @Override
    public List<SQLRecognizer> create(String sqlData, String dbType) {

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sqlData));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.SqlStatementsContext sqlStatementsContext = parser.sqlStatements();

        List<MySqlParser.SqlStatementContext> sqlStatementContexts = sqlStatementsContext.sqlStatement();

        List<SQLRecognizer> recognizers = null;
        SQLRecognizer recognizer = null;

        for (MySqlParser.SqlStatementContext sql : sqlStatementContexts) {

            StatementSqlVisitor visitor = new StatementSqlVisitor();

            String originalSQL = visitor.visit(sql).toString();

            SQLOperateRecognizerHolder recognizerHolder =
                    SQLOperateRecognizerHolderFactory.getSQLRecognizerHolder(dbType.toLowerCase());
            if (sql.dmlStatement().updateStatement() != null) {
                recognizer = recognizerHolder.getUpdateRecognizer(originalSQL);
            } else if (sql.dmlStatement().insertStatement() != null) {
                recognizer = recognizerHolder.getInsertRecognizer(originalSQL);
            } else if (sql.dmlStatement().deleteStatement() != null) {
                recognizer = recognizerHolder.getDeleteRecognizer(originalSQL);
            } else if (sql.dmlStatement().selectStatement() != null) {
                recognizer = recognizerHolder.getSelectForUpdateRecognizer(originalSQL);
            }

            if (recognizer != null) {
                if (recognizers == null) {
                    recognizers = new ArrayList<>();
                }
                recognizers.add(recognizer);
            }
        }
        return recognizers;
    }
}
