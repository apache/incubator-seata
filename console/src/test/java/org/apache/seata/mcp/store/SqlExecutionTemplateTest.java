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
package org.apache.seata.mcp.store;

import org.apache.seata.common.exception.StoreException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlExecutionTemplateTest {

    @Test
    void shouldExecuteSelectQueryWithWhereClauseWithoutParameters() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        String sql = "select name from users where id = 1";

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(1)).thenReturn("name");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject(1)).thenReturn("Alice");

        try (MockedStatic<DataSourceFactory> dataSourceFactory = mockStatic(DataSourceFactory.class)) {
            dataSourceFactory.when(() -> DataSourceFactory.getDataSource("biz")).thenReturn(dataSource);

            List<Map<String, Object>> rows = new SqlExecutionTemplate().query("biz", sql);

            assertEquals(1, rows.size());
            assertEquals("Alice", rows.get(0).get("name"));
        }
        verify(connection).prepareStatement(sql);
    }

    @Test
    void shouldRejectNonSelectSql() {
        StoreException exception = assertThrows(
                StoreException.class, () -> new SqlExecutionTemplate().query("biz", "delete from users where id = 1"));

        assertEquals(
                "The query valid failed,Only query operations are allowed：delete from users where id = 1",
                exception.getMessage());
    }
}
