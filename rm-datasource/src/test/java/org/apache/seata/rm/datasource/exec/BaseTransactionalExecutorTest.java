/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.exec;

import org.apache.seata.core.model.GlobalLockConfig;
import org.apache.seata.rm.GlobalLockExecutor;
import org.apache.seata.rm.GlobalLockTemplate;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseTransactionalExecutorTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testExecuteWithGlobalLockSet() throws Throwable {

        // initial objects
        ConnectionProxy connectionProxy = new ConnectionProxy(null, null);
        StatementProxy statementProxy = new StatementProxy<>(connectionProxy, null);

        BaseTransactionalExecutor<Object, Statement> baseTransactionalExecutor =
                new BaseTransactionalExecutor<Object, Statement>(statementProxy, null, (SQLRecognizer) null) {
                    @Override
                    protected Object doExecute(Object... args) {
                        return null;
                    }
                };
        GlobalLockTemplate template = new GlobalLockTemplate();

        // not in global lock context
        try {
            baseTransactionalExecutor.execute(new Object());
            Assertions.assertFalse(connectionProxy.isGlobalLockRequire(), "connection context set!");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // in global lock context
        template.execute(new GlobalLockExecutor() {
            @Override
            public Object execute() throws Throwable {
                baseTransactionalExecutor.execute(new Object());
                Assertions.assertTrue(connectionProxy.isGlobalLockRequire(), "connection context not set!");
                return null;
            }

            @Override
            public GlobalLockConfig getGlobalLockConfig() {
                return null;
            }
        });
    }

    @Test
    public void testBuildLockKey() {
        // build expect data
        String tableName = "test_name";
        String fieldOne = "1";
        String fieldTwo = "2";
        String split1 = ":";
        String split2 = ",";
        String pkColumnName = "id";
        // test_name:1,2
        String buildLockKeyExpect = tableName + split1 + fieldOne + split2 + fieldTwo;
        // mock field
        Field field1 = mock(Field.class);
        when(field1.getValue()).thenReturn(fieldOne);
        Field field2 = mock(Field.class);
        when(field2.getValue()).thenReturn(fieldTwo);
        List<Map<String, Field>> pkRows = new ArrayList<>();
        pkRows.add(Collections.singletonMap(pkColumnName, field1));
        pkRows.add(Collections.singletonMap(pkColumnName, field2));

        // mock tableMeta
        TableMeta tableMeta = mock(TableMeta.class);
        when(tableMeta.getTableName()).thenReturn(tableName);
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[] {pkColumnName}));
        // mock tableRecords
        TableRecords tableRecords = mock(TableRecords.class);
        when(tableRecords.getTableMeta()).thenReturn(tableMeta);
        when(tableRecords.size()).thenReturn(pkRows.size());
        when(tableRecords.pkRows()).thenReturn(pkRows);
        // mock executor
        BaseTransactionalExecutor executor = mock(BaseTransactionalExecutor.class);
        when(executor.buildLockKey(tableRecords)).thenCallRealMethod();
        when(executor.getTableMeta()).thenReturn(tableMeta);
        assertThat(executor.buildLockKey(tableRecords)).isEqualTo(buildLockKeyExpect);
    }

    @Test
    public void testBuildLockKeyWithMultiPk() {
        // build expect data
        String tableName = "test_name";
        String pkOneValue1 = "1";
        String pkOneValue2 = "2";
        String pkTwoValue1 = "one";
        String pkTwoValue2 = "two";
        String split1 = ":";
        String split2 = ",";
        String split3 = "_";
        String pkOneColumnName = "id";
        String pkTwoColumnName = "userId";
        // test_name:1_one,2_two
        String buildLockKeyExpect =
                tableName + split1 + pkOneValue1 + split3 + pkTwoValue1 + split2 + pkOneValue2 + split3 + pkTwoValue2;
        // mock field
        Field pkOneField1 = mock(Field.class);
        when(pkOneField1.getValue()).thenReturn(pkOneValue1);
        Field pkOneField2 = mock(Field.class);
        when(pkOneField2.getValue()).thenReturn(pkOneValue2);
        Field pkTwoField1 = mock(Field.class);
        when(pkTwoField1.getValue()).thenReturn(pkTwoValue1);
        Field pkTwoField2 = mock(Field.class);
        when(pkTwoField2.getValue()).thenReturn(pkTwoValue2);
        List<Map<String, Field>> pkRows = new ArrayList<>();
        Map<String, Field> row1 = new HashMap<String, Field>() {
            {
                put(pkOneColumnName, pkOneField1);
                put(pkTwoColumnName, pkTwoField1);
            }
        };
        pkRows.add(row1);
        Map<String, Field> row2 = new HashMap<String, Field>() {
            {
                put(pkOneColumnName, pkOneField2);
                put(pkTwoColumnName, pkTwoField2);
            }
        };
        pkRows.add(row2);

        // mock tableMeta
        TableMeta tableMeta = mock(TableMeta.class);
        when(tableMeta.getTableName()).thenReturn(tableName);
        when(tableMeta.getPrimaryKeyOnlyName())
                .thenReturn(Arrays.asList(new String[] {pkOneColumnName, pkTwoColumnName}));
        // mock tableRecords
        TableRecords tableRecords = mock(TableRecords.class);
        when(tableRecords.getTableMeta()).thenReturn(tableMeta);
        when(tableRecords.size()).thenReturn(pkRows.size());
        when(tableRecords.pkRows()).thenReturn(pkRows);
        // mock executor
        BaseTransactionalExecutor executor = mock(BaseTransactionalExecutor.class);
        when(executor.buildLockKey(tableRecords)).thenCallRealMethod();
        when(executor.getTableMeta()).thenReturn(tableMeta);
        assertThat(executor.buildLockKey(tableRecords)).isEqualTo(buildLockKeyExpect);
    }

    @Test
    public void testBuildLockKeyWithBinaryPrimaryKey() {
        // Test binary (byte[]) primary key handling
        String tableName = "test_binary_table";
        byte[] binaryPkValue1 = new byte[] {1, 2, 3, 15, -1}; // -1 represents 0xFF in signed byte
        byte[] binaryPkValue2 = new byte[] {10, 20, 30};
        String pkColumnName = "binary_id";

        // Expected: test_binary_table:[1, 2, 3, 15, -1],[10, 20, 30]
        // Using ArrayUtils.toString() format instead of memory address like [B@1b57bff9]
        // Note: byte is signed in Java, so 0xFF (255) is represented as -1
        String expectedLockKey = tableName + ":[1, 2, 3, 15, -1],[10, 20, 30]";

        // Mock fields with byte[] values
        Field binaryField1 = mock(Field.class);
        when(binaryField1.getValue()).thenReturn(binaryPkValue1);
        Field binaryField2 = mock(Field.class);
        when(binaryField2.getValue()).thenReturn(binaryPkValue2);

        List<Map<String, Field>> pkRows = new ArrayList<>();
        pkRows.add(Collections.singletonMap(pkColumnName, binaryField1));
        pkRows.add(Collections.singletonMap(pkColumnName, binaryField2));

        // Mock tableMeta
        TableMeta tableMeta = mock(TableMeta.class);
        when(tableMeta.getTableName()).thenReturn(tableName);
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[] {pkColumnName}));

        // Mock tableRecords
        TableRecords tableRecords = mock(TableRecords.class);
        when(tableRecords.getTableMeta()).thenReturn(tableMeta);
        when(tableRecords.size()).thenReturn(pkRows.size());
        when(tableRecords.pkRows()).thenReturn(pkRows);

        // Mock executor
        BaseTransactionalExecutor executor = mock(BaseTransactionalExecutor.class);
        when(executor.buildLockKey(tableRecords)).thenCallRealMethod();
        when(executor.getTableMeta()).thenReturn(tableMeta);

        String actualLockKey = executor.buildLockKey(tableRecords);

        // Verify that byte[] is properly converted to readable format
        assertThat(actualLockKey).isEqualTo(expectedLockKey);
        // Ensure it's not using memory address format
        assertThat(actualLockKey).doesNotContain("[B@]");
        assertThat(actualLockKey).contains("[1, 2, 3, 15, -1]");
        assertThat(actualLockKey).contains("[10, 20, 30]");
    }

    @Test
    public void testBuildLockKeyWithMixedPrimaryKeys() {
        // Test mixed primary keys: one regular string and one binary
        String tableName = "test_mixed_table";
        String stringPkValue = "user123";
        byte[] binaryPkValue = new byte[] {16, 32, 48, 64};
        String stringPkColumnName = "user_id";
        String binaryPkColumnName = "session_id";

        // Expected: test_mixed_table:user123_[16, 32, 48, 64]
        String expectedLockKey = tableName + ":user123_[16, 32, 48, 64]";

        // Mock fields
        Field stringField = mock(Field.class);
        when(stringField.getValue()).thenReturn(stringPkValue);
        Field binaryField = mock(Field.class);
        when(binaryField.getValue()).thenReturn(binaryPkValue);

        List<Map<String, Field>> pkRows = new ArrayList<>();
        Map<String, Field> row = new HashMap<String, Field>() {
            {
                put(stringPkColumnName, stringField);
                put(binaryPkColumnName, binaryField);
            }
        };
        pkRows.add(row);

        // Mock tableMeta
        TableMeta tableMeta = mock(TableMeta.class);
        when(tableMeta.getTableName()).thenReturn(tableName);
        when(tableMeta.getPrimaryKeyOnlyName())
                .thenReturn(Arrays.asList(new String[] {stringPkColumnName, binaryPkColumnName}));

        // Mock tableRecords
        TableRecords tableRecords = mock(TableRecords.class);
        when(tableRecords.getTableMeta()).thenReturn(tableMeta);
        when(tableRecords.size()).thenReturn(pkRows.size());
        when(tableRecords.pkRows()).thenReturn(pkRows);

        // Mock executor
        BaseTransactionalExecutor executor = mock(BaseTransactionalExecutor.class);
        when(executor.buildLockKey(tableRecords)).thenCallRealMethod();
        when(executor.getTableMeta()).thenReturn(tableMeta);

        String actualLockKey = executor.buildLockKey(tableRecords);

        // Verify mixed types are handled correctly
        assertThat(actualLockKey).isEqualTo(expectedLockKey);
        assertThat(actualLockKey).doesNotContain("[B@]");
        assertThat(actualLockKey).contains("user123");
        assertThat(actualLockKey).contains("[16, 32, 48, 64]");
    }
}
