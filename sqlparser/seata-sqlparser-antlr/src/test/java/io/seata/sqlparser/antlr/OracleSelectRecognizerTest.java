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

import io.seata.sqlparser.antlr.oracle.OracleContext;
import io.seata.sqlparser.antlr.oracle.listener.SelectSpecificationSqlListener;
import io.seata.sqlparser.antlr.oracle.parser.OracleLexer;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.stream.ANTLRNoCaseStringStream;
import io.seata.sqlparser.antlr.oracle.visitor.StatementSqlVisitor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author YechenGu
 */
public class OracleSelectRecognizerTest {

    private String baseStatementSqlVisitor(String sql) {
        OracleLexer lexer = new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser = new OracleParser(tokenStream);
        OracleParser.Sql_statementContext sqlStatementContext = parser.sql_statement();
        StatementSqlVisitor sqlVisitor = new StatementSqlVisitor();
        return sqlVisitor.visit(sqlStatementContext).toString();
    }

    @Test
    public void selectRecognizerTest_0(){
        String sql = "SELECT * FROM t1";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals("*", context.getQueryColumnNames().get(0).getColumnName());
    }

    @Test
    public void selectRecognizerTest_1(){
        String sql = "SELECT a FROM t1 b WHERE b.id = d";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals("b.id = d",context.getWhereCondition());
        Assertions.assertEquals("b",context.getTableAlias());
    }

    @Test
    public void selectRecognizerTest_2(){
        String sql = "SELECT name,age,phone FROM t1 WHERE id = 'id1'";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1", context.getTableName());
        Assertions.assertEquals("phone", context.getQueryColumnNames().get(2).getColumnName());
        Assertions.assertEquals("id = 'id1'", context.getWhereCondition());
        Assertions.assertEquals("id = 'id1'", context.getQueryWhereCondition().get(0).getQueryWhereColumn());
    }

    @Test
    public void selectRecognizerTest_3(){
        String sql = "SELECT name,phone FROM t1 WHERE id = 1 and username = '11' and age = 'a' or gyc = '1' or aa = 1";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1", context.getTableName());
        Assertions.assertEquals("name", context.getQueryColumnNames().get(0).getColumnName());
        Assertions.assertEquals("id = 1", context.getQueryWhereCondition().get(0).getQueryWhereColumn());
        Assertions.assertEquals("gyc = '1'", context.getQueryWhereCondition().get(3).getQueryWhereColumn());
    }

    @Test
    public void selectRecognizerTest_4(){
        String sql = "SELECT name1, name2 FROM t1 WHERE name = 1 and id between 2 and 3 or img = '11'";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new SelectSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1", context.getTableName());
        Assertions.assertEquals("name1", context.getQueryColumnNames().get(0).getColumnName());
        Assertions.assertEquals("id between 2 and 3", context.getQueryWhereCondition().get(1).getQueryWhereColumn());
    }
}
