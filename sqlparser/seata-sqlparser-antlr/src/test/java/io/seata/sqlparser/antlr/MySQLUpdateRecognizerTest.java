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
import io.seata.sqlparser.antlr.mysql.listener.UpdateSpecificationSqlListener;
import io.seata.sqlparser.antlr.mysql.parser.MySqlLexer;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import io.seata.sqlparser.antlr.mysql.stream.ANTLRNoCaseStringStream;
import io.seata.sqlparser.antlr.mysql.visit.StatementSqlVisitor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * The type My sql update recognizer test.
 */
public class MySQLUpdateRecognizerTest {

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
     * Update recognizer test 0.
     */
    @Test
    public void updateRecognizerTest_0() {

        String sql = "UPDATE t1 a SET a.name = 'name1' WHERE a.id = 'id1'";

        String visitorText = baseStatementSqlVisitor(sql);

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(visitorText));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals(1, mySqlContext.getUpdateForValues().size());
        Assertions.assertEquals("a.id", mySqlContext.getUpdateForWhereColumnNames().get(0).getUpdateWhereColumnName());
        Assertions.assertEquals("a.name", mySqlContext.getUpdateFoColumnNames().get(0).getUpdateColumn());
        Assertions.assertEquals("a.id = 'id1'", mySqlContext.getWhereCondition());
        Assertions.assertEquals("a", mySqlContext.getTableAlias());
    }

    /**
     * Update recognizer test 1.
     */
    @Test
    public void updateRecognizerTest_1() {

        String sql = "UPDATE t1 SET name1 = name1, name2 = name2 WHERE id = 'id1'";
        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals("name1", mySqlContext.getUpdateFoColumnNames().get(0).getUpdateColumn());
        Assertions.assertEquals("name1", mySqlContext.getUpdateForValues().get(0).getUpdateValue());
        Assertions.assertEquals("name2", mySqlContext.getUpdateFoColumnNames().get(1).getUpdateColumn());
        Assertions.assertEquals("name2", mySqlContext.getUpdateForValues().get(1).getUpdateValue());
        Assertions.assertEquals("id = 'id1'", mySqlContext.getWhereCondition());
    }

    /**
     * Update recognizer test 2.
     */
    @Test
    public void updateRecognizerTest_2() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = ?";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(mySqlContext), rootContext);

        List<MySqlContext.SQL> updateForWhereValColumnNames = mySqlContext.getUpdateForWhereValColumnNames();

        System.out.println(updateForWhereValColumnNames);

        Assertions.assertEquals("?", mySqlContext.getUpdateForWhereValColumnNames().get(0).getUpdateWhereValColumnName());
        Assertions.assertEquals("name1", mySqlContext.getUpdateFoColumnNames().get(0).getUpdateColumn());
        Assertions.assertEquals("name2", mySqlContext.getUpdateFoColumnNames().get(1).getUpdateColumn());
        Assertions.assertEquals("id = ?", mySqlContext.getWhereCondition());
    }

    /**
     * Update recognizer test 3.
     */
    @Test
    public void updateRecognizerTest_3() {

        String sql = "UPDATE t1 as t SET t.name1 = 'name1', t.name2 = 'name2' WHERE id in (1, 2)";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(mySqlContext), rootContext);

        Assertions.assertEquals("1", mySqlContext.getUpdateForWhereValColumnNames().get(0).getUpdateWhereValColumnName());
        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals("t.name1", mySqlContext.getUpdateFoColumnNames().get(0).getUpdateColumn());
        Assertions.assertEquals("'name1'", mySqlContext.getUpdateForValues().get(0).getUpdateValue());
        Assertions.assertEquals("t.name2", mySqlContext.getUpdateFoColumnNames().get(1).getUpdateColumn());
        Assertions.assertEquals("id in (1,2)", mySqlContext.getWhereCondition());
        Assertions.assertEquals("t", mySqlContext.getTableAlias());
    }

    /**
     * Update recognizer test 5.
     */
    @Test
    public void updateRecognizerTest_5() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id between 1 and 2";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext mySqlContext = new MySqlContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(mySqlContext), rootContext);
        Assertions.assertEquals("t1", mySqlContext.getTableName());
        Assertions.assertEquals("name1", mySqlContext.getUpdateFoColumnNames().get(0).getUpdateColumn());
        Assertions.assertEquals("'name1'", mySqlContext.getUpdateForValues().get(0).getUpdateValue());
        Assertions.assertEquals("name2", mySqlContext.getUpdateFoColumnNames().get(1).getUpdateColumn());
        Assertions.assertEquals("2", mySqlContext.getUpdateForWhereValColumnNames().get(1).getUpdateWhereValColumnName());
        Assertions.assertEquals("id between 1 and 2", mySqlContext.getWhereCondition());
    }
}