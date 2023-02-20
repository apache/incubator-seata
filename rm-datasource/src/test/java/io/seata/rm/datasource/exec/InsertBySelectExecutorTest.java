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
package io.seata.rm.datasource.exec;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

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
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.sql.struct.IndexMeta;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.mysql.MySQLInsertRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

/**
 * @author renliangyu857
 */
public class InsertBySelectExecutorTest {

    private StatementProxy statementProxy;

    private final String COLUMN_ID = "id";
    private final String COLUMN_A = "a";
    private final String COLUMN_B = "b";

    public void init() {
        List<String> returnValueColumnLabels = Lists.newArrayList("id", "a", "b");
        Object[][] returnValue = new Object[][] {new Object[] {1, 1, 1}, new Object[] {2, 2, 2},};
        Object[][] columnMetas = new Object[][] {
            new Object[] {"", "", "insert_select_t2", "id1", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO",
                "YES"},
            new Object[] {"", "", "insert_select_t2", "a1", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 2, "NO", "YES"},
            new Object[] {"", "", "insert_select_t2", "b1", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 2, "NO",
                "YES"}};
        Object[][] indexMetas = new Object[][] {new Object[] {"PRIMARY", "id", false, "", 3, 1, "A", 34},};

        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setDriver(mockDriver);

        DataSourceProxy dataSourceProxy = new DataSourceProxy(dataSource);
        try {
            Field field = dataSourceProxy.getClass().getDeclaredField("dbType");
            field.setAccessible(true);
            field.set(dataSourceProxy, "mysql");
            ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, dataSource.getConnection().getConnection());
            MockStatementBase mockStatement = new MockStatement(dataSource.getConnection().getConnection());
            statementProxy = new StatementProxy(connectionProxy, mockStatement);
        } catch (Exception e) {
            throw new RuntimeException("init failed");
        }
    }

    @Test
    public void testRecognizer() {
        String sql = "insert into insert_select_t select id1,a1,b1 from insert_select_t2";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        SQLInsertRecognizer recognizer = new MySQLInsertRecognizer(sql, asts.get(0));
        Assertions.assertEquals(SQLType.INSERT_SELECT, recognizer.getSQLType());
        Assertions.assertNotNull(recognizer.getSubQuerySql());
    }

    @Test
    public void testAfterImage() throws SQLException {
        init();
        MySQLInsertRecognizer insertRecognizer = mock(MySQLInsertRecognizer.class);
        InsertBySelectExecutor insertExecutor = Mockito.spy(new InsertBySelectExecutor(statementProxy, (statement, args) -> {
            return null;
        }, insertRecognizer));
        when(insertRecognizer.getTableName()).thenReturn("insert_select_t");
        when(insertRecognizer.getInsertColumns()).thenReturn(Arrays.asList("id", "a", "b"));
        when(insertRecognizer.getSubQuerySql()).thenReturn("select id1,a1,b1 from insert_select_t2");
        when(insertExecutor.getTableMeta(insertRecognizer.getTableName())).thenReturn(mockTableMeta());
        TableRecords afterImage = insertExecutor.afterImage(new TableRecords());
        Assertions.assertEquals(afterImage.getTableName(), "insert_select_t");
        List<Row> rows = afterImage.getRows();
        Assertions.assertEquals(2, rows.size());
    }

    private TableMeta mockTableMeta() {
        TableMeta tableMeta = new TableMeta();
        tableMeta.setTableName("insert_select_t");
        ColumnMeta columnIdMeta = new ColumnMeta();
        columnIdMeta.setTableName("insert_select_t");
        columnIdMeta.setColumnName(COLUMN_ID);
        columnIdMeta.setDataType(Types.INTEGER);
        tableMeta.getAllColumns().put(COLUMN_ID, columnIdMeta);

        columnIdMeta.setTableName("insert_select_t");
        columnIdMeta.setColumnName(COLUMN_A);
        columnIdMeta.setDataType(Types.INTEGER);
        tableMeta.getAllColumns().put(COLUMN_A, columnIdMeta);

        columnIdMeta.setTableName("insert_select_t");
        columnIdMeta.setColumnName(COLUMN_B);
        columnIdMeta.setDataType(Types.INTEGER);
        tableMeta.getAllColumns().put(COLUMN_B, columnIdMeta);

        IndexMeta primary = new IndexMeta();
        primary.setIndextype(IndexType.PRIMARY);
        primary.setValues(Lists.newArrayList(columnIdMeta));

        tableMeta.getAllIndexes().put(COLUMN_ID, primary);
        return tableMeta;
    }
}
