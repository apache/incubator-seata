package io.seata.sqlparser.druid;

import io.seata.sqlparser.antlr.mysql.MySqlContext;
import io.seata.sqlparser.antlr.mysql.parser.MySqlLexer;
import io.seata.sqlparser.antlr.mysql.parser.MySqlParser;
import io.seata.sqlparser.antlr.mysql.stream.ANTLRNoCaseStringStream;
import io.seata.sqlparser.antlr.mysql.visit.StatementSqlVisitor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author houzhi
 * @date 2020-7-10
 * @description
 */
public class MySQLInsertRecognizerTest {

    /**
     * Insert recognizer test 0.
     */
    @Test
    public void insertRecognizerTest_0() {

        String sql = "INSERT INTO t1 (name) VALUES ('name1')";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext visitorSqlContext = new MySqlContext();
        StatementSqlVisitor visitor = new StatementSqlVisitor(visitorSqlContext);
        visitor.visit(rootContext);

        Assertions.assertEquals("t1", visitorSqlContext.tableName);
        Assertions.assertEquals(Collections.singletonList("name"), Arrays.asList(visitorSqlContext.getInsertColumnNames().get(0).getInsertColumnName()));
        Assertions.assertEquals(1, visitorSqlContext.insertRows);
        Assertions.assertEquals(Collections.singletonList("name1"), visitorSqlContext.getInsertForValColumnNames().get(0));
    }

    /**
     * Insert recognizer test 1.
     */
    @Test
    public void insertRecognizerTest_1() {

        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 12)";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext visitorSqlContext = new MySqlContext();
        StatementSqlVisitor visitor = new StatementSqlVisitor(visitorSqlContext);
        visitor.visit(rootContext);

        Assertions.assertEquals("t1", visitorSqlContext.tableName);
        Assertions.assertEquals(Arrays.asList("name1", "name2"), visitorSqlContext.getInsertColumnNames().stream().map(insert -> {
            return insert.getInsertColumnName();
        }).collect(Collectors.toList()));
        Assertions.assertEquals(1, visitorSqlContext.insertRows);
        Assertions.assertEquals(Arrays.asList("name1", "12"), visitorSqlContext.getInsertForValColumnNames().get(0));
    }

    /**
     * Insert recognizer test 3.
     */
    @Test
    public void insertRecognizerTest_3() {

        String sql = "INSERT INTO t1 (name1, name2) VALUES ('name1', 'name2'), ('name3', 'name4'), ('name5', 'name6')";

        MySqlLexer lexer = new MySqlLexer(new ANTLRNoCaseStringStream(sql));

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        MySqlParser parser = new MySqlParser(tokenStream);

        MySqlParser.RootContext rootContext = parser.root();

        MySqlContext visitorSqlContext = new MySqlContext();
        StatementSqlVisitor visitor = new StatementSqlVisitor(visitorSqlContext);
        visitor.visit(rootContext);


        Assertions.assertEquals("t1", visitorSqlContext.tableName);
        Assertions.assertEquals("name2", visitorSqlContext.getInsertColumnNames().get(1).getInsertColumnName());

        Integer insertRows = visitorSqlContext.insertRows;
        Assertions.assertEquals(3, insertRows);

        Assertions.assertEquals(Arrays.asList("name1", "name2"), visitorSqlContext.getInsertForValColumnNames().get(0));
        Assertions.assertEquals(Arrays.asList("name3", "name4"), visitorSqlContext.getInsertForValColumnNames().get(1));
        Assertions.assertEquals(Arrays.asList("name5", "name6"), visitorSqlContext.getInsertForValColumnNames().get(2));
    }


//    INSERT ALL INTO ord_order_z_flow
//            (FLOW_ID, order_Id)
//    VALUES
//            ('123456577123457', '123456577123454') INTO ord_order_z_flow
//  (FLOW_ID, order_Id)
//    VALUES
//            ('123456577123458', '123456577123454') INTO ord_order_z_flow
//  (FLOW_ID, order_Id)
//    VALUES
//            ('123456577123459', '123456577123454') INTO ord_order_z_flow
//  (FLOW_ID, order_Id)
//    VALUES
//            ('123456577123460', '123456577123454') INTO ord_order_z_flow
//  (FLOW_ID, order_Id)
//    VALUES
//            ('123456577123461', '123456577123454')
//    SELECT 1 FROM DUAL)
}
