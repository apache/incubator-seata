package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.druid.mysql.MySQLReplaceRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MySQLReplaceRecognizerTest extends AbstractRecognizerTest {
    @Override
    public String getDbType() {
        return JdbcConstants.MYSQL;
    }

    /**
     * Replace recognizer test 0.
     */
    @Test
    public void replaceRecognizerTest_0() {
        String sql = "REPLACE INTO test (id, data, ts) VALUES (1, 'Old', '2014-08-20 18:47:00')";
        SQLStatement statement = getSQLStatement(sql);
        MySQLReplaceRecognizer mySQLReplaceRecognizer = new MySQLReplaceRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLReplaceRecognizer.getOriginalSQL());
        Assertions.assertEquals(true, mySQLReplaceRecognizer.selectQueryIsEmpty());
        Assertions.assertEquals("test", mySQLReplaceRecognizer.getTableName());
        Assertions.assertEquals(Arrays.asList("id", "data", "ts"), mySQLReplaceRecognizer.getReplaceColumns());
        Assertions.assertEquals(Collections.singletonList("1, 'Old', '2014-08-20 18:47:00'"), mySQLReplaceRecognizer.getReplaceValues());
    }

    /**
     * Replace recognizer test 1.
     */
    @Test
    public void replaceRecognizerTest_1() {
        String sql = "REPLACE INTO test SET column1 = value1, column2 = value2";
        SQLStatement statement = getSQLStatement(sql);
        MySQLReplaceRecognizer mySQLReplaceRecognizer = new MySQLReplaceRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLReplaceRecognizer.getOriginalSQL());
        Assertions.assertEquals(true, mySQLReplaceRecognizer.selectQueryIsEmpty());
        Assertions.assertEquals("test", mySQLReplaceRecognizer.getTableName());
        Assertions.assertEquals("column1", mySQLReplaceRecognizer.getReplaceColumns().get(0));
        List<String> allValues = mySQLReplaceRecognizer.getReplaceValues();
        String[] values = allValues.get(0).split(", ");
        Assertions.assertEquals("value1", values[0]);
    }

    /**
     * Replace recognizer test 2.
     */
    @Test
    public void replaceRecognizerTest_2() {
        String sql = "REPLACE INTO test(Name, City) SELECT Name, City FROM Person WHERE id = 2";
        SQLStatement statement = getSQLStatement(sql);
        MySQLReplaceRecognizer mySQLReplaceRecognizer = new MySQLReplaceRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLReplaceRecognizer.getOriginalSQL());
        Assertions.assertEquals(false, mySQLReplaceRecognizer.selectQueryIsEmpty());
        Assertions.assertEquals("SELECT Name, City FROM Person WHERE id = 2", mySQLReplaceRecognizer.getSelectQuery());
    }

    /**
     * Replace recognizer test 3.
     */
    @Test
    public void replaceRecognizerTest_3() {
        String sql = "REPLACE INTO test VALUES (1, 'Old', '2014-08-20 18:47:00')";
        SQLStatement statement = getSQLStatement(sql);
        MySQLReplaceRecognizer mySQLReplaceRecognizer = new MySQLReplaceRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLReplaceRecognizer.getOriginalSQL());
        Assertions.assertEquals(true, mySQLReplaceRecognizer.selectQueryIsEmpty());
        Assertions.assertEquals("test", mySQLReplaceRecognizer.getTableName());
        Assertions.assertEquals(null, mySQLReplaceRecognizer.getReplaceColumns());
        List<String> allValues = mySQLReplaceRecognizer.getReplaceValues();
        String[] values = allValues.get(0).split(", ");
        Assertions.assertEquals("1", values[0]);
    }

}
