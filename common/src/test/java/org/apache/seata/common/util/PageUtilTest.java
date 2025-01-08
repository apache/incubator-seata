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
package org.apache.seata.common.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;

/**
 * The page util test.
 */
public class PageUtilTest {

    private int validPageNum;
    private int validPageSize;
    private String validTimeColumnName;
    @InjectMocks
    private PageUtil pageUtil;

    @BeforeEach
    void setUp() {
        validPageNum = 1;
        validPageSize = 10;
        validTimeColumnName = "gmt_create";
        MockitoAnnotations.initMocks(this);

    }

    @Test
    public void testPageSql() {
        String sourceSql = "select * from test where a = 1";

        String mysqlTargetSql = "select * from test where a = 1 limit 5 offset 0";

        String oracleTargetSql = "select * from " +
            "( select ROWNUM rn, temp.* from (select * from test where a = 1) temp )" +
            " where rn between 1 and 5";
        String sqlserverTargetSql = "select * from (select temp.*, ROW_NUMBER() OVER(ORDER BY gmt_create desc) AS rowId from (select * from test where a = 1) temp ) t where t.rowId between 1 and 5";

        assertEquals(PageUtil.pageSql(sourceSql, "mysql", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "h2", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "postgresql", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "oceanbase", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "dm", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "oscar", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "oracle", 1, 5), oracleTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "sqlserver", 1, 5), sqlserverTargetSql);

        assertThrows(NotSupportYetException.class, () -> PageUtil.pageSql(sourceSql, "xxx", 1, 5));
    }

    @Test
    void testCountSql() {
        String sourceSql = "select * from test where a = 1";

        String targetSql = "select count(1) from test where a = 1";

        assertEquals(PageUtil.countSql(sourceSql, "mysql"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "h2"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "postgresql"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "oceanbase"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "dm"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "oscar"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "oracle"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "sqlserver"), targetSql);

        assertThrows(NotSupportYetException.class, () -> PageUtil.countSql(sourceSql, "xxx"));
    }

    @Test
    void checkParamValidPageParams() {
        assertDoesNotThrow(() -> PageUtil.checkParam(validPageNum, validPageSize));
    }

    @Test
    void checkParamPageNumBelowMin() {
        int invalidPageNum = PageUtil.MIN_PAGE_NUM - 1;
        assertThrows(IllegalArgumentException.class, () -> PageUtil.checkParam(invalidPageNum, validPageSize));
    }

    @Test
    void checkParamPageNumAboveMax() {
        int invalidPageNum = PageUtil.MAX_PAGE_NUM + 1;
        assertThrows(IllegalArgumentException.class, () -> PageUtil.checkParam(invalidPageNum, validPageSize));
    }

    @Test
    void checkParamPageSizeBelowMin() {
        int invalidPageSize = PageUtil.MIN_PAGE_SIZE - 1;
        assertThrows(IllegalArgumentException.class, () -> PageUtil.checkParam(validPageNum, invalidPageSize));
    }

    @Test
    void checkParamPageSizeAboveMax() {
        int invalidPageSize = PageUtil.MAX_PAGE_SIZE + 1;
        assertThrows(IllegalArgumentException.class, () -> PageUtil.checkParam(validPageNum, invalidPageSize));
    }

    @Test
    void setObjectWithDateParameterSetsDateCorrectly() throws SQLException {
        List<Object> params = new ArrayList<>();
        params.add(new Date(System.currentTimeMillis()));
        params.add(123);

        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        doNothing().when(preparedStatement).setDate(anyInt(), any(java.sql.Date.class));

        PageUtil.setObject(preparedStatement, params);

        Mockito.verify(preparedStatement).setDate(anyInt(), any(java.sql.Date.class));
    }

    @Test
    void setObjectWithNonDateParameterSetsObjectCorrectly() throws SQLException {
        List<Object> params = new ArrayList<>();
        params.add("testString");

        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);

        PageUtil.setObject(preparedStatement, params);

        Mockito.verify(preparedStatement, Mockito.times(1)).setObject(anyInt(), any());
    }

    @Test
    void setObjectEmptyListNoInteractionWithPreparedStatement() throws SQLException {
        List<Object> params = new ArrayList<>();

        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);

        PageUtil.setObject(preparedStatement, params);

        Mockito.verify(preparedStatement, Mockito.never()).setObject(anyInt(), any());
    }

    @Test
    void setObjectNullListNoInteraction() throws SQLException {
        List<Object> params = null;
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);

        assertThrows(NullPointerException.class, () -> PageUtil.setObject(preparedStatement, params));
    }

    @Test
    public void getTimeStartSqlSupportedDBTypes() {
        String[] supportedDBTypes = {"mysql", "oracle", "postgresql", "sqlserver", "dm", "oscar"};
        String expectedSQL = " and FLOOR(gmt_create/1000) >= ? ";

        for (String dbType : supportedDBTypes) {
            assertEquals(expectedSQL, PageUtil.getTimeStartSql(dbType, validTimeColumnName));
        }
    }

    @Test
    public void getTimeStartSqlNotSupportedDBType() {
        String notSupportedDBType = "xxx";
        assertThrows(IllegalArgumentException.class, () -> PageUtil.getTimeStartSql(notSupportedDBType, validTimeColumnName));
    }

}
