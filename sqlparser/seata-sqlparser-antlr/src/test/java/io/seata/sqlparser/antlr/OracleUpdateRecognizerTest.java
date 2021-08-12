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
import io.seata.sqlparser.antlr.oracle.listener.UpdateSpecificationSqlListener;
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
public class OracleUpdateRecognizerTest {

    private String baseStatementSqlVisitor(String sql) {
        OracleLexer lexer = new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser = new OracleParser(tokenStream);
        OracleParser.Sql_statementContext sqlStatementContext = parser.sql_statement();
        StatementSqlVisitor sqlVisitor = new StatementSqlVisitor();
        return sqlVisitor.visit(sqlStatementContext).toString();
    }

    @Test
    public void updateRecognizerTest_0(){
        String sql = "UPDATE t1 a SET a.name = 'name1' WHERE a.id = 'id1'";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals("a",context.getTableAlias());
        Assertions.assertEquals("a.id = 'id1'",context.getWhereCondition());
    }

    @Test
    public void updateRecognizerTest_1(){
        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = ?";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals("name1",context.getUpdateColumnNames().get(0).getUpdateColumn());
        Assertions.assertEquals("'name1'",context.getUpdateColumnValues().get(0).getUpdateValue());
        Assertions.assertEquals("id = ?",context.getWhereCondition());
    }

    @Test
    public void updateRecognizerTest_2(){
        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id between 1 and 2 and name3 = 'name3'";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new UpdateSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals("'name2'",context.getUpdateColumnValues().get(1).getUpdateValue());
        Assertions.assertEquals("id between 1 and 2",context.getUpdateWhereCondition().get(0).getUpdateWhereColumn());
    }
}
