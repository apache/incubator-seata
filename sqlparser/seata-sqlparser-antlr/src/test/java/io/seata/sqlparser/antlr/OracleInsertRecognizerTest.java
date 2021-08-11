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
import io.seata.sqlparser.antlr.oracle.parser.OracleLexer;
import io.seata.sqlparser.antlr.oracle.parser.OracleParser;
import io.seata.sqlparser.antlr.oracle.stream.ANTLRNoCaseStringStream;
import io.seata.sqlparser.antlr.oracle.visitor.InsertStatementSqlVisitor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author YechenGu
 */
public class OracleInsertRecognizerTest {

    @Test
    public void insertRecognizerTest_0(){
        String sql = "INSERT INTO t1 (id) VALUES (1)";
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        InsertStatementSqlVisitor insertStatementSqlVisitor = new InsertStatementSqlVisitor(context);
        insertStatementSqlVisitor.visit(scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals(1, context.getInsertRows());
        Assertions.assertEquals(("id"), context.getInsertColumnNames().get(0).getInsertColumnName());
        Assertions.assertEquals(("1"), context.getInsertForValColumnNames().get(0));
    }

    @Test
    public void insertRecognizerTest_1(){
        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 12)";
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        InsertStatementSqlVisitor insertStatementSqlVisitor = new InsertStatementSqlVisitor(context);
        insertStatementSqlVisitor.visit(scriptContext);

        Assertions.assertEquals("t1",context.getTableName());
        Assertions.assertEquals(2, context.getInsertRows());
        Assertions.assertEquals("name1", context.getInsertColumnNames().get(0).getInsertColumnName());
        Assertions.assertEquals("12", context.getInsertForValColumnNames().get(1));
    }

    @Test
    public void insertRecognizerTest_2(){
        String sql = "INSERT INTO t2 (name1, name2, name3) VALUES ('name1', 12, 255)";
        OracleLexer lexer= new OracleLexer(new ANTLRNoCaseStringStream(sql));
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        OracleParser parser= new OracleParser(tokenStream);

        OracleParser.Sql_scriptContext scriptContext = parser.sql_script();
        OracleContext context = new OracleContext();
        InsertStatementSqlVisitor insertStatementSqlVisitor = new InsertStatementSqlVisitor(context);
        insertStatementSqlVisitor.visit(scriptContext);

        Assertions.assertEquals("t2",context.getTableName());
        Assertions.assertEquals(3, context.getInsertRows());
        Assertions.assertEquals("name3", context.getInsertColumnNames().get(2).getInsertColumnName());
        Assertions.assertEquals("255", context.getInsertForValColumnNames().get(2));
    }

}
