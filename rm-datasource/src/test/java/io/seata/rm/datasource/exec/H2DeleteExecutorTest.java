package io.seata.rm.datasource.exec;

import com.alibaba.druid.mock.MockStatement;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.google.common.collect.Lists;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.druid.h2.H2DeleteRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * @author hongyan
 */
public class H2DeleteExecutorTest {

    private static DeleteExecutor deleteExecutor;

    private static StatementProxy statementProxy;

    @BeforeAll
    public static void init() {
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "name");
        Object[][] returnValue = new Object[][] {
            new Object[] {1, "Tom"},
            new Object[] {2, "Jack"},
        };
        Object[][] columnMetas = new Object[][] {
            new Object[] {"", "", "table_delete_executor_test", "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
            new Object[] {"", "", "table_delete_executor_test", "name", Types.VARCHAR, "VARCHAR", 64, 0, 10, 0, "", "", 0, 0, 64, 2, "YES", "NO"},
        };
        Object[][] indexMetas = new Object[][] {
            new Object[] {"PRIMARY_KEY", "id", false, "", 3, 1, "A", 34},
        };

        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
        try {
            Field field = dataSourceProxy.getClass().getDeclaredField("dbType");
            field.setAccessible(true);
            field.set(dataSourceProxy, "h2");
            ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());
            MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
            statementProxy = new StatementProxy(connectionProxy, mockStatement);
        } catch (Exception e) {
            throw new RuntimeException("init failed");
        }
        String sql = "delete from t where id = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.H2);
        H2DeleteRecognizer recognizer = new H2DeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> {
            return null;
        }, recognizer);
    }

    @Test
    public void testBeforeImage() throws SQLException {
        Assertions.assertNotNull(deleteExecutor.beforeImage());

        String sql = "delete from t";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.H2);
        H2DeleteRecognizer recognizer = new H2DeleteRecognizer(sql, asts.get(0));
        deleteExecutor = new DeleteExecutor(statementProxy, (statement, args) -> null, recognizer);
        Assertions.assertNotNull(deleteExecutor.beforeImage());
    }

    @Test
    public void testAfterImage() throws SQLException {
        TableRecords tableRecords = deleteExecutor.beforeImage();
        Assertions.assertEquals(0, deleteExecutor.afterImage(tableRecords).size());
    }
}
