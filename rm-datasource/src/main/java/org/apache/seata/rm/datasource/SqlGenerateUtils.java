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
package org.apache.seata.rm.datasource;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.sqlparser.util.ColumnUtils;

/**
 * generate sql and set value to sql
 *
 */
public class SqlGenerateUtils {

    private static final int MAX_IN_SIZE = 1000;

    private SqlGenerateUtils() {

    }

    /**
     * build full sql by pks.
     * @param sqlPrefix sql prefix
     * @param suffix sql suffix
     * @param pkNameList pk column name list
     * @param rowSize the row size of records
     * @param dbType the type of database
     * @return full sql
     */
    public static String buildSQLByPKs(String sqlPrefix, String suffix, List<String> pkNameList, int rowSize, String dbType) {
        List<WhereSql> whereList = buildWhereConditionListByPKs(pkNameList, rowSize, dbType, MAX_IN_SIZE);
        StringJoiner sqlJoiner = new StringJoiner(" UNION ");
        whereList.forEach(whereSql -> sqlJoiner.add(sqlPrefix + " " + whereSql.getSql() + " " + suffix));
        return sqlJoiner.toString();
    }
    /**
     * each pk is a condition.the result will like :" [(id,userCode) in ((?,?),(?,?)), (id,userCode) in ((?,?),(?,?)
     * ), (id,userCode) in ((?,?))]"
     * Build where condition by pks string. size default MAX_IN_SIZE
     *
     * @param pkNameList pk column name list
     * @param rowSize    the row size of records
     * @param dbType     the type of database
     * @return return where condition sql list.the sql can search all related records not just one.
     */
    public static List<WhereSql> buildWhereConditionListByPKs(List<String> pkNameList, int rowSize, String dbType) {
        return buildWhereConditionListByPKs(pkNameList, rowSize, dbType, MAX_IN_SIZE);
    }
    /**
     * each pk is a condition.the result will like :" [(id,userCode) in ((?,?),(?,?)), (id,userCode) in ((?,?),(?,?)
     * ), (id,userCode) in ((?,?))]"
     * Build where condition by pks string.
     *
     * @param pkNameList pk column name list
     * @param rowSize    the row size of records
     * @param dbType     the type of database
     * @param maxInSize  the max in size
     * @return return where condition sql list.the sql can search all related records not just one.
     */
    public static List<WhereSql> buildWhereConditionListByPKs(List<String> pkNameList, int rowSize, String dbType, int maxInSize) {
        List<WhereSql> whereSqls = new ArrayList<>();
        //we must consider the situation of composite primary key
        int batchSize = rowSize % maxInSize == 0 ? rowSize / maxInSize : (rowSize / maxInSize) + 1;
        for (int batch = 0; batch < batchSize; batch++) {
            StringBuilder whereStr = new StringBuilder();
            whereStr.append("(");
            for (int i = 0; i < pkNameList.size(); i++) {
                if (i > 0) {
                    whereStr.append(",");
                }
                whereStr.append(ColumnUtils.addEscape(pkNameList.get(i), dbType));
            }
            whereStr.append(") in ( ");

            int eachSize = (batch == batchSize - 1) ? (rowSize % maxInSize == 0 ? maxInSize : rowSize % maxInSize)
                : maxInSize;
            for (int i = 0; i < eachSize; i++) {
                //each row is a bracket
                if (i > 0) {
                    whereStr.append(",");
                }
                whereStr.append("(");
                for (int x = 0; x < pkNameList.size(); x++) {
                    if (x > 0) {
                        whereStr.append(",");
                    }
                    whereStr.append("?");
                }
                whereStr.append(")");
            }
            whereStr.append(" )");
            whereSqls.add(new WhereSql(whereStr.toString(), eachSize, pkNameList.size()));
        }

        return whereSqls;
    }

    /**
     * set parameter for PreparedStatement, this is only used in pk sql.
     *
     * @param pkRowsList pkRowsList
     * @param pkColumnNameList pkColumnNameList
     * @param pst preparedStatement
     * @throws SQLException SQLException
     */
    public static void setParamForPk(List<Map<String, Field>> pkRowsList, List<String> pkColumnNameList,
                                     PreparedStatement pst) throws SQLException {
        int paramIndex = 1;
        for (int i = 0; i < pkRowsList.size(); i++) {
            Map<String, Field> rowData = pkRowsList.get(i);
            for (String columnName : pkColumnNameList) {
                Field pkField = rowData.get(columnName);
                pst.setObject(paramIndex, pkField.getValue(), pkField.getType());
                paramIndex++;
            }
        }
    }

    /**
     * each pk is a condition.the result will like :" id =? and userCode =?"
     *
     * @param pkNameList pkNameList
     * @param dbType dbType
     * @return return where condition sql string.the sql can just search one related record.
     */
    public static String buildWhereConditionByPKs(List<String> pkNameList, String dbType) {
        StringBuilder whereStr = new StringBuilder();
        //we must consider the situation of composite primary key
        for (int i = 0; i < pkNameList.size(); i++) {
            if (i > 0) {
                whereStr.append(" and ");
            }
            String pkName = pkNameList.get(i);
            whereStr.append(ColumnUtils.addEscape(pkName, dbType));
            whereStr.append(" = ? ");
        }
        return whereStr.toString();
    }

    public static class WhereSql {
        /**
         * sql
         */
        private final String sql;

        /**
         * row size
         */
        private final int rowSize;

        /**
         * pk size
         */
        private final int pkSize;

        public WhereSql(String sql, int rowSize, int pkSize) {
            this.sql = sql;
            this.rowSize = rowSize;
            this.pkSize = pkSize;
        }

        public String getSql() {
            return sql;
        }

        public int getRowSize() {
            return rowSize;
        }

        public int getPkSize() {
            return pkSize;
        }
    }
}
