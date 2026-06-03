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
package org.apache.seata.mcp.service;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.mcp.core.constant.SqlConstant;
import org.apache.seata.mcp.core.props.BusinessDataSourcesProperties;
import org.apache.seata.mcp.entity.vo.BusinessQueryResult;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.apache.seata.mcp.store.SqlSafetyValidator;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MysqlMetadataServiceTest {

    @Test
    void shouldQueryTablesWithSchemaParameter() {
        SqlExecutionTemplate sqlExecutionTemplate = mock(SqlExecutionTemplate.class);
        BusinessDataSourcesProperties properties = mock(BusinessDataSourcesProperties.class);
        BusinessQueryResult result = result(row("TABLE_NAME", "orders", "TABLE_COMMENT", "business orders"));
        when(properties.isAllowedSchema("business-ds://biz", "app")).thenReturn(true);
        when(sqlExecutionTemplate.trustedQuery("business-ds://biz", SqlConstant.GET_TABLE_NAME_SQL, "app"))
                .thenReturn(result);

        MysqlMetadataService service =
                new MysqlMetadataService(sqlExecutionTemplate, new SqlSafetyValidator(), properties);

        assertEquals(
                "orders", service.listTables("business-ds://biz", "app").get(0).getTableName());
        verify(sqlExecutionTemplate).trustedQuery("business-ds://biz", SqlConstant.GET_TABLE_NAME_SQL, "app");
    }

    @Test
    void shouldRejectExplainForUnsafeSql() {
        MysqlMetadataService service = new MysqlMetadataService(
                mock(SqlExecutionTemplate.class), new SqlSafetyValidator(), mock(BusinessDataSourcesProperties.class));

        assertThrows(StoreException.class, () -> service.explainSql("business-ds://biz", "delete from users"));
    }

    private BusinessQueryResult result(Map<String, Object> row) {
        BusinessQueryResult result = new BusinessQueryResult();
        result.setRows(Collections.singletonList(row));
        result.setRowCount(1);
        return result;
    }

    private Map<String, Object> row(Object... values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            row.put(String.valueOf(values[i]), values[i + 1]);
        }
        return row;
    }
}
