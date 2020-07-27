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
package io.seata.sqlparser.antlr.mysql;

import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.antlr.SQLOperateRecognizerHolder;
import io.seata.sqlparser.antlr.SQLOperateRecognizerHolderFactory;
import io.seata.sqlparser.antlr.mysql.listener.SqlSpecificationListener;
import io.seata.sqlparser.antlr.mysql.parser.MySqlLexer;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import io.seata.sqlparser.antlr.mysql.stream.ANTLRNoCaseStringStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

/**
 * AntlrMySQLRecognizerFactory
 *
 * @author zhihou
 */
class AntlrMySQLRecognizerFactory implements SQLRecognizerFactory {

    @Override
    public List<SQLRecognizer> create(String sqlData, String dbType) {

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sqlData));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();
        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SqlSpecificationListener(mySqlContext), rootContext);

        List<MySqlContext.SQL> sqlInfos = mySqlContext.getSqlInfos();

        List<SQLRecognizer> recognizers = null;
        SQLRecognizer recognizer = null;

        for(MySqlContext.SQL sql : sqlInfos){

            SQLOperateRecognizerHolder recognizerHolder =
                    SQLOperateRecognizerHolderFactory.getSQLRecognizerHolder(dbType.toLowerCase());
            if(sql.getSqlType() == SQLType.UPDATE.value()){
                recognizer = recognizerHolder.getUpdateRecognizer(sql.getSql());
            }else if(sql.getSqlType() == SQLType.INSERT.value()){
                recognizer = recognizerHolder.getInsertRecognizer(sql.getSql());
            }else if(sql.getSqlType() == SQLType.DELETE.value()){
                recognizer = recognizerHolder.getDeleteRecognizer(sql.getSql());
            }else if(sql.getSqlType() == SQLType.SELECT.value()){
                recognizer = recognizerHolder.getSelectForUpdateRecognizer(sql.getSql());
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
