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
import java.util.Date;
import java.util.List;

import org.apache.seata.common.exception.NotSupportYetException;

/**
 * db page util
 *
 */
public class PageUtil {
    /**
     * The constant MIN_PAGE_NUM
     */
    public static final int MIN_PAGE_NUM = 1;
    /**
     * The constant MAX_PAGE_NUM
     */
    public static final int MAX_PAGE_NUM = 999;
    /**
     * The constant MIN_PAGE_SIZE
     */
    public static final int MIN_PAGE_SIZE = 1;
    /**
     * The constant MAX_PAGE_SIZE
     */
    public static final int MAX_PAGE_SIZE = 100;
    /**
     * The constant SOURCE_SQL_PLACE_HOLD
     */
    private static final String SOURCE_SQL_PLACE_HOLD = " #sourcesql# ";
    /**
     * The constant LIMIT_PLACE_HOLD
     */
    private static final String LIMIT_PLACE_HOLD = " #limit# ";
    /**
     * The constant OFFSET_PLACE_HOLD
     */
    private static final String OFFSET_PLACE_HOLD = " #offset# ";
    /**
     * The constant START_PLACE_HOLD
     */
    private static final String START_PLACE_HOLD = " #start# ";
    /**
     * The constant END_PLACE_HOLD
     */
    private static final String END_PLACE_HOLD = " #end# ";
    /**
     * The constant LIMIT_TEMPLAGE.
     */
    private static final String LIMIT_TEMPLATE = SOURCE_SQL_PLACE_HOLD + " limit " + LIMIT_PLACE_HOLD + " offset "
            + OFFSET_PLACE_HOLD;
    /**
     * The constant ORACLE_PAGE_TEMPLATE.
     */
    private static final String ORACLE_PAGE_TEMPLATE = "select * from ( select ROWNUM rn, temp.* from ("
            + SOURCE_SQL_PLACE_HOLD + ") temp ) where rn between " + START_PLACE_HOLD + " and " + END_PLACE_HOLD;

    /**
     * The constant SQLSERVER_PAGE_TEMPLATE. Currently, it only works for order-by condition of "ORDER BY gmt_create desc"
     */
    private static final String SQLSERVER_PAGE_TEMPLATE = "select * from (select temp.*, ROW_NUMBER() OVER(ORDER BY gmt_create desc) AS rowId from ("
            + SOURCE_SQL_PLACE_HOLD + ") temp ) t where t.rowId between " + START_PLACE_HOLD + " and " + END_PLACE_HOLD;
    /**
     * check page parm
     *
     * @param pageNum the page num
     * @param pageSize the page size
     */
    public static void checkParam(int pageNum, int pageSize) {
        if (!(pageNum >= MIN_PAGE_NUM && pageNum <= MAX_PAGE_NUM)) {
            throw new IllegalArgumentException("pageNum range not in [" + MIN_PAGE_NUM + "-" + MAX_PAGE_NUM + "]");
        }
        if (!(pageSize >= MIN_PAGE_SIZE && pageSize <= MAX_PAGE_SIZE)) {
            throw new IllegalArgumentException("pageSize range not in [" + MIN_PAGE_SIZE + "-" + MAX_PAGE_SIZE + "]");
        }
    }

    /**
     * get pagesql
     *
     * @param sourceSql the source sql
     * @param dbType the db type
     * @param pageNum the page num
     * @param pageSize the page size
     * @return the page sql
     */
    public static String pageSql(String sourceSql, String dbType, int pageNum, int pageSize) {
        switch (dbType) {
            case "mysql":
            case "h2":
            case "postgresql":
            case "kingbase":
            case "oceanbase":
            case "dm":
            case "oscar":
                return LIMIT_TEMPLATE.replace(SOURCE_SQL_PLACE_HOLD, sourceSql)
                        .replace(LIMIT_PLACE_HOLD, String.valueOf(pageSize))
                        .replace(OFFSET_PLACE_HOLD, String.valueOf((pageNum - 1) * pageSize));
            case "oracle":
                return ORACLE_PAGE_TEMPLATE.replace(SOURCE_SQL_PLACE_HOLD, sourceSql)
                        .replace(START_PLACE_HOLD, String.valueOf(pageSize * (pageNum - 1) + 1))
                        .replace(END_PLACE_HOLD, String.valueOf(pageSize * pageNum));
            case "sqlserver":
                return SQLSERVER_PAGE_TEMPLATE.replace(SOURCE_SQL_PLACE_HOLD, sourceSql)
                        .replace(START_PLACE_HOLD, String.valueOf(pageSize * (pageNum - 1) + 1))
                        .replace(END_PLACE_HOLD, String.valueOf(pageSize * pageNum));
            default:
                throw new NotSupportYetException("PageUtil not support this dbType:" + dbType);
        }
    }

    /**
     * get countsql
     *
     * @param sourceSql the source sql
     * @param dbType the db type
     * @return the count sql
     */
    public static String countSql(String sourceSql, String dbType) {
        switch (dbType) {
            case "mysql":
            case "h2":
            case "oceanbase":
            case "oracle":
            case "dm":
            case "oscar":
                return sourceSql.replaceAll("(?i)(?<=select)(.*)(?=from)", " count(1) ");
            case "postgresql":
            case "kingbase":
            case "sqlserver":
                int lastIndexOfOrderBy = sourceSql.toLowerCase().lastIndexOf("order by");
                if (lastIndexOfOrderBy != -1) {
                    return sourceSql.substring(0, lastIndexOfOrderBy).replaceAll("(?i)(?<=select)(.*)(?=from)", " count(1) ");
                }
                return sourceSql.replaceAll("(?i)(?<=select)(.*)(?=from)", " count(1) ");
            default:
                throw new NotSupportYetException("PageUtil not support this dbType:" + dbType);
        }
    }

    /**
     * set sqlParamList in preparedStatement
     * @param ps the prepared statement
     * @param sqlParamList the sql param list
     * @throws SQLException the sql exception
     */
    public static void setObject(PreparedStatement ps, List<Object> sqlParamList) throws SQLException {
        for (int i = 0; i < sqlParamList.size(); i++) {
            if (sqlParamList.get(i) instanceof Date) {
                ps.setDate(i + 1, new java.sql.Date(((Date) sqlParamList.get(i)).getTime()));
            } else {
                ps.setObject(i + 1, sqlParamList.get(i));
            }
        }
    }

    /**
     * get sql for time start
     * @param dbType
     * @param timeColumnName
     * @return java.lang.String
     */
    public static String getTimeStartSql(String dbType, String timeColumnName) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "oracle":
            case "postgresql":
            case "sqlserver":
            case "dm":
            case "oscar":
                return " and FLOOR(" + timeColumnName + "/1000) >= ? ";
            default:
                throw new IllegalArgumentException("The DB type :" + dbType + " is not supported yet");
        }
    }

    /**
     * get sql for time end
     * @param dbType
     * @param timeColumnName
     * @return java.lang.String
     */
    public static String getTimeEndSql(String dbType, String timeColumnName) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "oracle":
            case "postgresql":
            case "sqlserver":
            case "dm":
            case "oscar":
                return " and FLOOR(" + timeColumnName + "/1000) <= ? ";
            default:
                throw new IllegalArgumentException("The DB type :" + dbType + " is not supported yet");
        }
    }
}
