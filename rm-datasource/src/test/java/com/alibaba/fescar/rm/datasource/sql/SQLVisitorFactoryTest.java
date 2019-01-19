package com.alibaba.fescar.rm.datasource.sql;

import com.alibaba.druid.util.JdbcConstants;
import org.junit.Assert;
import org.junit.Test;

public class SQLVisitorFactoryTest {
    @Test
    public void testMysqlVisitor() {
        String sql = "/*!mycat:schema=wz_buss_001*/ select * from buss_user_info";
        SQLRecognizer recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assert.assertTrue(recognizer.getSqlHints().size() == 1);
        Assert.assertTrue(recognizer.getSQLType() == SQLType.SELECT_FOR_UPDATE);
    }
}
