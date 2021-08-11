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
package io.seata.sqlparser.antlr.oracle;

import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.antlr.SQLOperateRecognizerHolder;
import io.seata.sqlparser.antlr.SQLOperateRecognizerHolderFactory;
import io.seata.sqlparser.antlr.oracle.parser.OracleLexer;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.stream.ANTLRNoCaseStringStream;
import io.seata.sqlparser.antlr.oracle.visitor.StatementSqlVisitor;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YechenGu
 */
public class AntlrOracleRecognizerFactory implements SQLRecognizerFactory {
    @Override
    public List<SQLRecognizer> create(String sqlData, String dbType) {

        OracleLexer lexer = new OracleLexer(new ANTLRNoCaseStringStream(sqlData));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        OracleParser parser = new OracleParser(tokenStream);

        OracleParser.StatementContext statementContext = parser.statement();

        OracleParser.Sql_statementContext sql = statementContext.sql_statement();

        List<SQLRecognizer> recognizers = null;
        SQLRecognizer recognizer = null;

        StatementSqlVisitor visitor = new StatementSqlVisitor();

        String originalSQL = visitor.visit(sql).toString();

        SQLOperateRecognizerHolder recognizerHolder =
                SQLOperateRecognizerHolderFactory.getSQLRecognizerHolder(dbType.toLowerCase());
        if (sql.data_manipulation_language_statements().update_statement() != null) {
            recognizer = recognizerHolder.getUpdateRecognizer(originalSQL);
        } else if (sql.data_manipulation_language_statements().insert_statement() != null) {
            recognizer = recognizerHolder.getInsertRecognizer(originalSQL);
        } else if (sql.data_manipulation_language_statements().delete_statement() != null) {
            recognizer = recognizerHolder.getDeleteRecognizer(originalSQL);
        } else if (sql.data_manipulation_language_statements().select_statement() != null) {
            recognizer = recognizerHolder.getSelectForUpdateRecognizer(originalSQL);
        }

        if (recognizer != null) {
            if (recognizers == null) {
                recognizers = new ArrayList<>();
            }
            recognizers.add(recognizer);
        }
        return recognizers;
    }
}
