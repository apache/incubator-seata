package io.seata.sqlparser.druid.sqlserver;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.druid.AbstractRecognizerTest;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type SqlServer operate holder test.
 *
 * @author GoodBoyCoder
 */
public class SqlServerOperateRecognizerHolderTest extends AbstractRecognizerTest {

    @Override
    public String getDbType() {
        return JdbcConstants.SQLSERVER;
    }

    @Test
    public void getDeleteRecognizerTest() {
        String sql = "DELETE FROM t1 WHERE id = 'id1'";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new SqlServerOperateRecognizerHolder().getDeleteRecognizer(sql, sqlStatement));
    }
    @Test
    public void getInsertRecognizerTest() {
        String sql = "INSERT INTO t (name) VALUES ('name1')";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new SqlServerOperateRecognizerHolder().getInsertRecognizer(sql, sqlStatement));
    }
    @Test
    public void getUpdateRecognizerTest() {
        String sql = "UPDATE t1 SET name = 'name1' WHERE id = 'id1'";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new SqlServerOperateRecognizerHolder().getUpdateRecognizer(sql, sqlStatement));
    }
    @Test
    public void getSelectForUpdateTest() {
        //test with lock
        String sql = "SELECT name FROM t1 WITH (ROWLOCK, UPDLOCK) WHERE id = 'id1'";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new SqlServerOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));

        //test with no lock
        sql = "SELECT name FROM t1 WHERE id = 'id1'";
        sqlStatement = getSQLStatement(sql);
        Assertions.assertNull(new SqlServerOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));
    }
}
