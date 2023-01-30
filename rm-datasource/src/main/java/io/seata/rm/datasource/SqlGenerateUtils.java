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
package io.seata.rm.datasource;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import io.seata.rm.datasource.sql.struct.Field;
import io.seata.sqlparser.util.ColumnUtils;

/**
 * generate sql and set value to sql
 *
 * @author JerryYin
 */
public class SqlGenerateUtils {

    private static final int MAX_IN_SIZE = 1000;

    private SqlGenerateUtils() {

    }

    public static String buildWhereConditionByPKs(List<String> pkNameList, int rowSize, String dbType)
        throws SQLException {
        return buildWhereConditionByPKs(pkNameList, rowSize, dbType, MAX_IN_SIZE);

    }
    /**
     * each pk is a condition.the result will like :" (id,userCode) in ((?,?),(?,?)) or (id,userCode) in ((?,?),(?,?)
     * ) or (id,userCode) in ((?,?))"
     * Build where condition by pks string.
     *
     * @param pkNameList pk column name list
     * @param rowSize    the row size of records
     * @param dbType     the type of database
     * @param maxInSize  the max in size
     * @return return where condition sql string.the sql can search all related records not just one.
     * @throws SQLException the sql exception
     */
    public static String buildWhereConditionByPKs(List<String> pkNameList, int rowSize, String dbType, int maxInSize)
        throws SQLException {
        StringBuilder whereStr = new StringBuilder();
        //we must consider the situation of composite primary key
        int batchSize = rowSize % maxInSize == 0 ? rowSize / maxInSize : (rowSize / maxInSize) + 1;
        for (int batch = 0; batch < batchSize; batch++) {
            if (batch > 0) {
                whereStr.append(" or ");
            }
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
        }

        return whereStr.toString();
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

}
