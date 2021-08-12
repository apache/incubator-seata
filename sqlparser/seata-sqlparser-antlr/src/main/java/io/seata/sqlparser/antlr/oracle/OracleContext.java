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
package io.seata.sqlparser.antlr.oracle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YechenGu
 */
public class OracleContext {
    /**
     * Table Name
     */
    public String tableName;

    /**
     * Table Alias
     */
    public String tableAlias;

    /**
     * Number of inserts
     */
    public Integer insertRows;

    /**
     * Where condition
     */
    private String whereCondition;

    /**
     * Query column name collection
     */
    public List<OracleContext.SQL> queryColumnNames = new ArrayList<>();

    /**
     * Conditional query column collection
     */
    public List<OracleContext.SQL> queryWhereCondition = new ArrayList<>();

    /**
     * Query column name collection
     */
    public List<OracleContext.SQL> insertColumnNames = new ArrayList<>();

    /**
     * Insert the value set corresponding to the column name
     */
    public List<String> insertForValColumnNames = new ArrayList<>();

    /**
     * Delete condition column set
     */
    public List<OracleContext.SQL> deleteWhereCondition = new ArrayList<>();

    /**
     * Conditional update condition column object collection
     */
    public List<OracleContext.SQL> updateWhereCondition = new ArrayList<>();

    /**
     * Update column name object value collection
     */
    public List<OracleContext.SQL> updateColumnNames = new ArrayList<>();


    /**
     * Update object value collection
     */
    public List<OracleContext.SQL> updateColumnValues = new ArrayList<>();

    /**
     * sql object information collection
     */
    public List<OracleContext.SQL> sqlInfos = new ArrayList<>();

    /**
     * originalSQL
     */
    private String originalSQL;

    public void addSqlInfo(OracleContext.SQL sql) {
        sqlInfos.add(sql);
    }

    public void addForInsertColumnName(String columnName) {
        OracleContext.SQL sql = new OracleContext.SQL();
        sql.setInsertColumnName(columnName);
        insertColumnNames.add(sql);
    }

    public void addForInsertValColumnName(String columnName) {
        insertForValColumnNames.add(columnName);
    }

    public void addQueryColumnNames(String columnName) {
        OracleContext.SQL sql = new OracleContext.SQL();
        sql.setColumnName(columnName);
        queryColumnNames.add(sql);
    }

    public void addQueryWhereColumn(String column) {
        OracleContext.SQL sql = new OracleContext.SQL();
        sql.setQueryWhereColumn(column);
        queryWhereCondition.add(sql);
    }

    public void addDeleteWhereColumn(String column) {
        OracleContext.SQL sql = new OracleContext.SQL();
        sql.setDeleteWhereColumn(column);
        deleteWhereCondition.add(sql);
    }

    public void addUpdateWhereColumn(String column) {
        OracleContext.SQL sql = new OracleContext.SQL();
        sql.setUpdateWhereColumn(column);
        updateWhereCondition.add(sql);
    }

    public void addUpdateColumnNames(String columnName) {
        SQL sql = new OracleContext.SQL();
        sql.setUpdateColumn(columnName);
        updateColumnNames.add(sql);
    }

    public void addUpdateValues(String columnName) {
        SQL sql = new OracleContext.SQL();
        sql.setUpdateValue(columnName);
        updateColumnValues.add(sql);
    }


    public static class SQL {
        private String columnName;
        private String tableName;
        private String queryWhereCondition;
        private String queryWhereColumn;
        private String insertColumnName;
        private String deleteWhereCondition;
        private String deleteWhereColumn;
        private String updateWhereCondition;
        private String updateWhereColumn;
        private String updateColumn;
        private String updateValue;
        private Integer sqlType;
        private String sql;

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getQueryWhereCondition() {
            return queryWhereCondition;
        }

        public void setQueryWhereCondition(String queryWhereCondition) {
            this.queryWhereCondition = queryWhereCondition;
        }

        public String getQueryWhereColumn() {
            return queryWhereColumn;
        }

        public void setQueryWhereColumn(String queryWhereColumn) {
            this.queryWhereColumn = queryWhereColumn;
        }

        public String getInsertColumnName() {
            return insertColumnName;
        }

        public void setInsertColumnName(String insertColumnName) {
            this.insertColumnName = insertColumnName;
        }

        public String getDeleteWhereCondition() {
            return deleteWhereCondition;
        }

        public void setDeleteWhereCondition(String deleteWhereCondition) {
            this.deleteWhereCondition = deleteWhereCondition;
        }

        public String getDeleteWhereColumn() {
            return deleteWhereColumn;
        }

        public void setDeleteWhereColumn(String deleteWhereColumn) {
            this.deleteWhereColumn = deleteWhereColumn;
        }

        public String getUpdateWhereCondition() {
            return updateWhereCondition;
        }

        public void setUpdateWhereCondition(String updateWhereCondition) {
            this.updateWhereCondition = updateWhereCondition;
        }

        public String getUpdateWhereColumn() {
            return updateWhereColumn;
        }

        public void setUpdateWhereColumn(String updateWhereColumn) {
            this.updateWhereColumn = updateWhereColumn;
        }

        public String getUpdateColumn() {
            return updateColumn;
        }

        public void setUpdateColumn(String updateColumn) {
            this.updateColumn = updateColumn;
        }

        public String getUpdateValue() {
            return updateValue;
        }

        public void setUpdateValue(String updateValue) {
            this.updateValue = updateValue;
        }

        public Integer getSqlType() {
            return sqlType;
        }

        public void setSqlType(Integer sqlType) {
            this.sqlType = sqlType;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

    }

    public List<OracleContext.SQL> getQueryColumnNames() {
        return queryColumnNames;
    }

    public List<OracleContext.SQL> getQueryWhereCondition() {
        return queryWhereCondition;
    }

    public List<OracleContext.SQL> getInsertColumnNames() {
        return insertColumnNames;
    }

    public List<String> getInsertForValColumnNames() {
        return insertForValColumnNames;
    }

    public List<OracleContext.SQL> getDeleteWhereCondition() {
        return deleteWhereCondition;
    }

    public List<OracleContext.SQL> getUpdateWhereCondition() {
        return updateWhereCondition;
    }

    public List<SQL> getUpdateColumnNames() {
        return updateColumnNames;
    }

    public List<SQL> getUpdateColumnValues() {
        return updateColumnValues;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getInsertRows() {
        return insertRows;
    }

    public void setInsertRows(Integer insertRows) {
        this.insertRows = insertRows;
    }

    public String getWhereCondition() {
        return whereCondition;
    }

    public void setWhereCondition(String whereCondition) {
        this.whereCondition = whereCondition;
    }

    public List<OracleContext.SQL> getSqlInfos() {
        return sqlInfos;
    }

    public String getOriginalSQL() {
        return originalSQL;
    }

    public void setOriginalSQL(String originalSQL) {
        this.originalSQL = originalSQL;
    }

    public void setTableAlias(String tableAlias) {
        this.tableAlias = tableAlias;
    }

    public String getTableAlias() {
        return tableAlias;
    }
}
