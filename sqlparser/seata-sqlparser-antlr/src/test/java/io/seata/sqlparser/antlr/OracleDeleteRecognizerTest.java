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
import io.seata.sqlparser.antlr.oracle.listener.DeleteSpecificationSqlListener;
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
public class OracleDeleteRecognizerTest {

    private String baseStatementSqlVisitor(String sql) {
        OracleLexer lexer = new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser = new OracleParser(tokenStream);
        OracleParser.Sql_statementContext sqlStatementContext = parser.sql_statement();
        StatementSqlVisitor sqlVisitor = new StatementSqlVisitor();
        return sqlVisitor.visit(sqlStatementContext).toString();
    }

    @Test
    public void deleteRecognizerTest_0(){
        String sql = "DELETE FROM t1 t WHERE t.id = 'id1'";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals("t",context.getTableAlias());
        Assertions.assertEquals("t.id = 'id1'",context.getWhereCondition());
    }

    @Test
    public void deleteRecognizerTest_1(){
        String sql = "DELETE FROM t1 WHERE id = ?";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals("id = ?",context.getWhereCondition());
        Assertions.assertEquals("id = ?",context.getDeleteWhereCondition().get(0).getDeleteWhereColumn());
    }

    @Test
    public void deleteRecognizerTest_2(){
        String sql = "DELETE FROM t1 t WHERE sid between 1 AND 10 and tid IN (1,2)";
        String sqlVisitor = baseStatementSqlVisitor(sql);
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sqlVisitor));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new DeleteSpecificationSqlListener(context),scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals("sid between 1 AND 10",context.getDeleteWhereCondition().get(0).getDeleteWhereColumn());
        Assertions.assertEquals("tid IN (1,2)",context.getDeleteWhereCondition().get(1).getDeleteWhereColumn());
    }
}
