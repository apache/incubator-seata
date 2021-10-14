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
package io.seata.sqlparser.antlr;

import io.seata.sqlparser.antlr.mysql.MySqlContext;
import io.seata.sqlparser.antlr.mysql.listener.SelectSpecificationSqlListener;
import io.seata.sqlparser.antlr.mysql.parser.MySqlLexer;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import io.seata.sqlparser.antlr.mysql.stream.ANTLRNoCaseStringStream;
import io.seata.sqlparser.antlr.mysql.visit.StatementSqlVisitor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author houzhi
 * @date 2020-7-9
 * @description
 */
public class MySQLSelectForUpdateRecognizerForListenerTest {

    /**
     * base statementSql visitor test
     */
    private String baseStatementSqlVisitor(String sql) {
        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        MySqlParser parser = new MySqlParser(tokenStream);
        MySqlParser.SqlStatementContext sqlStatementContext = parser.sqlStatement();
        StatementSqlVisitor sqlToStringVisitor = new StatementSqlVisitor();
        return sqlToStringVisitor.visit(sqlStatementContext).toString();
    }

    /**
     * Select for update recognizer test 0.
     */
    @Test
    public void selectForUpdateRecognizerTest_0() {

        String sql = "SELECT a FROM t1 b WHERE b.id = d FOR UPDATE";

        String visitorText = baseStatementSqlVisitor(sql);

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(visitorText));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext listenerSqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(listenerSqlContext), rootContext);

        Assertions.assertEquals("t1", listenerSqlContext.getTableName());
        Assertions.assertEquals("b.id = d", listenerSqlContext.getWhereCondition());
        Assertions.assertEquals("b", listenerSqlContext.getTableAlias());
    }

    /**
     * Select for update recognizer test 1.
     */
    @Test
    public void selectForUpdateRecognizerTest_1() {

        String sql = "SELECT name,age,phone FROM t1 WHERE id = 'id1' FOR UPDATE";

        String visitorText = baseStatementSqlVisitor(sql);

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(visitorText));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext listenerSqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(listenerSqlContext), rootContext);

        Assertions.assertEquals("t1", listenerSqlContext.getTableName());
        Assertions.assertEquals("phone", listenerSqlContext.getQueryColumnNames().get(2).getColumnName());
        Assertions.assertEquals("id = 'id1'", listenerSqlContext.getWhereCondition());
    }

    /**
     * Select for update recognizer test 1.
     */
    @Test
    public void selectForUpdateRecognizerTest_2() {

        String sql = "SELECT name,age,phone FROM t2 WHERE id = 'id'FOR UPDATE";

        String visitorText = baseStatementSqlVisitor(sql);

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(visitorText));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("t2", mySqlContext.getTableName());
        Assertions.assertEquals("phone", mySqlContext.getQueryColumnNames().get(2).getColumnName());
        Assertions.assertEquals("id = 'id'", mySqlContext.getWhereCondition());
        Assertions.assertEquals("id", mySqlContext.getQueryWhereColumnNames().get(0).getQueryWhereColumnName());
        Assertions.assertEquals("'id'", mySqlContext.getQueryWhereValColumnNames().get(0).getQueryWhereValColumnName());
    }

    /**
     * Select for update recognizer test 1.
     */
    @Test
    public void selectForUpdateRecognizerTest_3() {

        String sql = "SELECT name,phone FROM t1 WHERE id = 1 and username = '11' and age = 'a' or hz = '1' or aa = 1 FOR UPDATE";

        String visitorText = baseStatementSqlVisitor(sql);

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(visitorText));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(mySqlContext), rootContext);


        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals("name", mySqlContext.getQueryColumnNames().get(0).getColumnName());
        Assertions.assertEquals("username", mySqlContext.getQueryWhereColumnNames().get(1).getQueryWhereColumnName());
        Assertions.assertEquals("'a'", mySqlContext.getQueryWhereValColumnNames().get(2).getQueryWhereValColumnName());
        Assertions.assertEquals("id = 1 and username = '11' and age = 'a' or hz = '1' or aa = 1", mySqlContext.getWhereCondition());
    }

    /**
     * Select for update recognizer test 4.
     */
    @Test
    public void selectForUpdateRecognizerTest_4() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id IN (100,101) FOR UPDATE";

        String visitorText = baseStatementSqlVisitor(sql);

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(visitorText));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals("name1", mySqlContext.getQueryColumnNames().get(0).getColumnName());
        Assertions.assertEquals("id", mySqlContext.getQueryWhereColumnNames().get(0).getQueryWhereColumnName());
        Assertions.assertEquals("101", mySqlContext.getQueryWhereValColumnNames().get(1).getQueryWhereValColumnName());
        Assertions.assertEquals("id IN (100,101)", mySqlContext.getWhereCondition());
    }

    /**
     * Select for update recognizer test 5.
     */
    @Test
    public void selectForUpdateRecognizerTest_5() {

        String sql = "SELECT name1, name2 FROM t1 WHERE name = 1 and id between 2 and 3 or img = '11' FOR UPDATE";

        String visitorText = baseStatementSqlVisitor(sql);

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(visitorText));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals("name1", mySqlContext.getQueryColumnNames().get(0).getColumnName());
        Assertions.assertEquals("id", mySqlContext.getQueryWhereColumnNames().get(1).getQueryWhereColumnName());
        Assertions.assertEquals("3", mySqlContext.getQueryWhereValColumnNames().get(2).getQueryWhereValColumnName());
        Assertions.assertEquals("name = 1 and id between 2 and 3 or img = '11'", mySqlContext.getWhereCondition());
    }
}