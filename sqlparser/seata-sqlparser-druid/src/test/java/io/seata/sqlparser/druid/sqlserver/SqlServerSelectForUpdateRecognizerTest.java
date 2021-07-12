package io.seata.sqlparser.druid.sqlserver;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.AbstractRecognizerTest;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * @author GoodBoyCoder
 */
public class SqlServerSelectForUpdateRecognizerTest extends AbstractRecognizerTest {
    @Override
    public String getDbType() {
        return JdbcConstants.SQLSERVER;
    }

    /**
     * Select for update recognizer test 0.(test with constant)
     */
    @Test
    public void selectForUpdateRecognizerTest_0() {
        String sql = "SELECT name FROM t1 WITH (ROWLOCK, UPDLOCK) WHERE id = 'id1' ";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals(SQLType.SELECT_FOR_UPDATE, sqlServerUpdateRecognizer.getSQLType());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals("id = 'id1'", sqlServerUpdateRecognizer.getWhereCondition());
    }

    /**
     * Select for update recognizer test 1.(test with placeholder)
     */
    @Test
    public void selectForUpdateRecognizerTest_1() {
        String sql = "SELECT name FROM t1 WITH (ROWLOCK, UPDLOCK) WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(SQLType.SELECT_FOR_UPDATE, sqlServerUpdateRecognizer.getSQLType());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Collections.singletonList("id1")), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Select for update recognizer test 2.(test with multi column)
     */
    @Test
    public void selectForUpdateRecognizerTest_2() {
        String sql = "SELECT name1, name2 FROM t1 WITH (ROWLOCK, UPDLOCK) WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(SQLType.SELECT_FOR_UPDATE, sqlServerUpdateRecognizer.getSQLType());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Collections.singletonList("id1")), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Select for update recognizer test 3.(test with IN sql)
     */
    @Test
    public void selectForUpdateRecognizerTest_3() {

        String sql = "SELECT name1, name2 FROM t1 WITH (ROWLOCK, UPDLOCK) WHERE id IN (?,?)";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    /**
     * Select for update recognizer test 4.(test with between...and... sql)
     */
    @Test
    public void selectForUpdateRecognizerTest_4() {

        String sql = "SELECT name1, name2 FROM t1 WITH (ROWLOCK, UPDLOCK) WHERE id between ? and ?";

        SQLStatement statement = getSQLStatement(sql);
        SqlServerSelectForUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }

    @Test
    public void testGetWhereCondition_1() {
        String sql = "SELECT * FROM t WITH (ROWLOCK, UPDLOCK)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.SQLSERVER);

        SqlServerSelectForUpdateRecognizer recognizer = new SqlServerSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);

        //test for select was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t WITH (ROWLOCK, UPDLOCK)";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.SQLSERVER);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.setSelect(null);
            new SqlServerSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        //test for query was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.SQLSERVER);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.getSelect().setQuery(null);
            new SqlServerSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });
    }

    @Test
    public void testGetSqlType() {
        String sql = "SELECT * FROM t WITH (ROWLOCK, UPDLOCK) WHERE id = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.SQLSERVER);

        SqlServerSelectForUpdateRecognizer recognizer = new SqlServerSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetTableAlias() {
        //test for no alias
        String sql = "SELECT * FROM t WITH (ROWLOCK, UPDLOCK) WHERE id = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.SQLSERVER);

        SqlServerSelectForUpdateRecognizer recognizer = new SqlServerSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());

        //test for alias
        sql = "SELECT * FROM t t1 WITH (ROWLOCK, UPDLOCK) WHERE id = ?";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.SQLSERVER);

        recognizer = new SqlServerSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "SELECT * FROM t WITH (ROWLOCK, UPDLOCK)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.SQLSERVER);

        SqlServerSelectForUpdateRecognizer recognizer = new SqlServerSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals("t", recognizer.getTableName());

        //test for alias
        sql = "SELECT * FROM t t1 WITH (ROWLOCK, UPDLOCK)";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.SQLSERVER);
        recognizer = new SqlServerSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals("t", recognizer.getTableName());
    }
}
