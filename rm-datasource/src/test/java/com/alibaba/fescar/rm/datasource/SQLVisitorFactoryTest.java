package com.alibaba.fescar.rm.datasource;

import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLVisitorFactory;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLDeleteRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLInsertRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLSelectForUpdateRecognizer;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLUpdateRecognizer;
import org.junit.Assert;
import org.junit.Test;

public class SQLVisitorFactoryTest {

    @Test
    public void testSqlRecognizing() {
        String dbType = JdbcConstants.MYSQL;
        String sql = "select a, b, c from t1 where id = 9";
        SQLRecognizer recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertNull(recognizer);

        sql = sql + " for update";
        recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertTrue(recognizer instanceof MySQLSelectForUpdateRecognizer);
    }

    @Test
    public void testSqlRecognizingWithHints() {
        String dbType = JdbcConstants.MYSQL;

        String sql = "/*!mycat:schema=demo_order*/ select a, b, c from t1 where id = 9";
        SQLRecognizer recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertNull(recognizer);

        sql = sql + " for update";
        recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertTrue(recognizer instanceof MySQLSelectForUpdateRecognizer);
        Assert.assertTrue(recognizer.getSqlHints().size() == 1);
        Assert.assertTrue(recognizer.getSqlHints().get(0).equals("!mycat:schema=demo_order"));

        sql = "/*!mycat:schema=demo_order*/ update t1 set a='test' where id = 9";
        recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertTrue(recognizer instanceof MySQLUpdateRecognizer);
        Assert.assertTrue(recognizer.getSqlHints().size() == 1);
        Assert.assertTrue(recognizer.getSqlHints().get(0).equals("!mycat:schema=demo_order"));

        sql = "/*!mycat:schema=demo_order*/ delete from t1 where id = 9";
        recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertTrue(recognizer instanceof MySQLDeleteRecognizer);
        Assert.assertTrue(recognizer.getSqlHints().size() == 1);
        Assert.assertTrue(recognizer.getSqlHints().get(0).equals("!mycat:schema=demo_order"));

        sql = "/*!mycat:schema=demo_order*/ insert into t1(a,b,c) values('v_a','v_b','v_c')";
        recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertTrue(recognizer instanceof MySQLInsertRecognizer);
        Assert.assertTrue(recognizer.getSqlHints().size() == 1);
        Assert.assertTrue(recognizer.getSqlHints().get(0).equals("!mycat:schema=demo_order"));


    }
}
