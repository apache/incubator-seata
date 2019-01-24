package com.alibaba.fescar.rm.datasource;

import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLVisitorFactory;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLSelectForUpdateRecognizer;
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
}
