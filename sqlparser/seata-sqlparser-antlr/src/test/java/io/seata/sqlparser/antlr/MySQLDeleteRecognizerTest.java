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
import io.seata.sqlparser.antlr.mysql.listener.DeleteSpecificationSqlListener;
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
 * @date 2020-7-10
 * @description
 */
public class MySQLDeleteRecognizerTest {

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
     * Delete recognizer test 0.
     */
    @Test
    public void deleteRecognizerTest_0() {

        String sql = "DELETE t FROM t1 as t WHERE t.id = 'id1'";

        String visitorText = baseStatementSqlVisitor(sql);

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(visitorText));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals("t.id = 'id1'", mySqlContext.getWhereCondition());
        Assertions.assertEquals("t", mySqlContext.getTableAlias());
    }


    /**
     * Delete recognizer test 1.
     */
    @Test
    public void deleteRecognizerTest_1() {

        String sql = "DELETE FROM t1 WHERE id = 'id1'";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("'id1'",
                mySqlContext.getDeleteForWhereValColumnNames().get(0).getDeleteWhereValColumnName());
        Assertions.assertEquals("id = 'id1'", mySqlContext.getWhereCondition());
    }


    /**
     * Delete recognizer test 1.
     */
    @Test
    public void deleteRecognizerTest_2() {

        String sql = "DELETE FROM t1 WHERE id = ?";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("?",
                mySqlContext.getDeleteForWhereValColumnNames().get(0).getDeleteWhereValColumnName());
        Assertions.assertEquals("id = ?", mySqlContext.getWhereCondition());
    }

    /**
     * Delete recognizer test 2.
     */
    @Test
    public void deleteRecognizerTest_3() {

        String sql = "DELETE FROM t1 WHERE id IN (1, 2)";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals("2", mySqlContext.getDeleteForWhereValColumnNames().get(1).getDeleteWhereValColumnName());
        Assertions.assertEquals("id IN (1,2)", mySqlContext.getWhereCondition());
    }

    /**
     * Delete recognizer test 3.
     */
    @Test
    public void deleteRecognizerTest_4() {

        String sql = "DELETE FROM t1 WHERE id between 1 AND 'id'";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("'id'", mySqlContext.getDeleteForWhereValColumnNames().get(1).getDeleteWhereValColumnName());
        Assertions.assertEquals("id between 1 AND 'id'", mySqlContext.getWhereCondition());
    }
}
