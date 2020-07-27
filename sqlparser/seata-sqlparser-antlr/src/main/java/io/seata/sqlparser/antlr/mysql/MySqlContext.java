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
package io.seata.sqlparser.antlr.mysql;

import java.util.ArrayList;
import java.util.List;

/**
 * @author houzhi
 */
public class MySqlContext {

    /**
     * 表名
     */
    public String tableName;

    /**
     * 插入条数
     */
    public Integer insertRows;

    /**
     * where条件
     */
    private String whereCondition;

    /**
     * 查询列名集合
     */
    public List<SQL> queryColumnNames = new ArrayList<>();

    /**
     * where 查询列名条件集合
     */
    public List<SQL> queryWhereColumnNames = new ArrayList<>();

    /**
     * where 查询列名对应值集合
     */
    public List<SQL> queryWhereValColumnNames = new ArrayList<>();

    /**
     * 查询列名集合
     */
    public List<SQL> insertColumnNames = new ArrayList<>();

    /**
     * 插入列表对象值集合
     */
    public List<List<String>> insertForValColumnNames = new ArrayList<>();

    /**
     * 删除条件列表集合
     */
    public List<SQL> deleteForWhereColumnNames = new ArrayList<>();

    /**
     * 删除列表对象值集合
     */
    public List<SQL> deleteForWhereValColumnNames = new ArrayList<>();

    /**
     * 更新条件列表集合
     */
    public List<SQL> updateForWhereColumnNames = new ArrayList<>();

    /**
     * 更新列表对象值集合
     */
    public List<SQL> updateForWhereValColumnNames = new ArrayList<>();

    /**
     * 更新列表对象值集合
     */
    public List<SQL> updateFoColumnNames = new ArrayList<>();


    /**
     * 更新列表对象值集合
     */
    public List<SQL> updateForValues = new ArrayList<>();

    /**
     * sql信息
     */
    public List<SQL> sqlInfos = new ArrayList<>();

    public void addSqlInfo(SQL sql) {
        sqlInfos.add(sql);
    }

    public void addForInsertColumnName(String columnName) {
        SQL sql = new SQL();
        sql.setInsertColumnName(columnName);
        insertColumnNames.add(sql);
    }

    public void addForInsertValColumnName(List<String> columnName) {
        insertForValColumnNames.add(columnName);
    }

    public void addQueryColumnNames(String columnName) {
        SQL sql = new SQL();
        sql.setColumnName(columnName);
        queryColumnNames.add(sql);
    }

    public void addQueryWhereColumnNames(String columnName) {
        SQL sql = new SQL();
        sql.setQueryWhereColumnName(columnName);
        queryWhereColumnNames.add(sql);
    }

    public void addQueryWhereValColumnNames(String columnName) {
        SQL sql = new SQL();
        sql.setQueryWhereValColumnName(columnName);
        queryWhereValColumnNames.add(sql);
    }

    public void addDeleteWhereColumnNames(String columnName) {
        SQL sql = new SQL();
        sql.setDeleteWhereColumnName(columnName);
        deleteForWhereColumnNames.add(sql);
    }

    public void addDeleteWhereValColumnNames(String columnName) {
        SQL sql = new SQL();
        sql.setDeleteWhereValColumnName(columnName);
        deleteForWhereValColumnNames.add(sql);
    }


    public void addUpdateWhereValColumnNames(String columnName) {
        SQL sql = new SQL();
        sql.setUpdateWhereValColumnName(columnName);
        updateForWhereValColumnNames.add(sql);
    }


    public void addUpdateWhereColumnNames(String columnName) {
        SQL sql = new SQL();
        sql.setUpdateWhereColumnName(columnName);
        updateForWhereColumnNames.add(sql);
    }

    public void addUpdateColumnNames(String columnName) {
        SQL sql = new SQL();
        sql.setUpdateColumn(columnName);
        updateFoColumnNames.add(sql);
    }

    public void addUpdateValues(String columnName) {
        SQL sql = new SQL();
        sql.setUpdateValue(columnName);
        updateForValues.add(sql);
    }

    public static class SQL {
        private String columnName;
        private String tableName;
        private String queryWhereValColumnName;
        private String queryWhereColumnName;
        private String insertColumnName;
        private String deleteWhereValColumnName;
        private String deleteWhereColumnName;
        private String updateWhereValColumnName;
        private String updateWhereColumnName;
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

        public String getQueryWhereValColumnName() {
            return queryWhereValColumnName;
        }

        public void setQueryWhereValColumnName(String queryWhereValColumnName) {
            this.queryWhereValColumnName = queryWhereValColumnName;
        }

        public String getQueryWhereColumnName() {
            return queryWhereColumnName;
        }

        public void setQueryWhereColumnName(String queryWhereColumnName) {
            this.queryWhereColumnName = queryWhereColumnName;
        }

        public String getInsertColumnName() {
            return insertColumnName;
        }

        public void setInsertColumnName(String insertColumnName) {
            this.insertColumnName = insertColumnName;
        }

        public String getDeleteWhereValColumnName() {
            return deleteWhereValColumnName;
        }

        public void setDeleteWhereValColumnName(String deleteWhereValColumnName) {
            this.deleteWhereValColumnName = deleteWhereValColumnName;
        }

        public String getDeleteWhereColumnName() {
            return deleteWhereColumnName;
        }

        public void setDeleteWhereColumnName(String deleteWhereColumnName) {
            this.deleteWhereColumnName = deleteWhereColumnName;
        }

        public String getUpdateWhereValColumnName() {
            return updateWhereValColumnName;
        }

        public void setUpdateWhereValColumnName(String updateWhereValColumnName) {
            this.updateWhereValColumnName = updateWhereValColumnName;
        }

        public String getUpdateWhereColumnName() {
            return updateWhereColumnName;
        }

        public void setUpdateWhereColumnName(String updateWhereColumnName) {
            this.updateWhereColumnName = updateWhereColumnName;
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

    public List<SQL> getQueryColumnNames() {
        return queryColumnNames;
    }

    public List<SQL> getQueryWhereColumnNames() {
        return queryWhereColumnNames;
    }

    public List<SQL> getQueryWhereValColumnNames() {
        return queryWhereValColumnNames;
    }

    public List<SQL> getInsertColumnNames() {
        return insertColumnNames;
    }

    public List<List<String>> getInsertForValColumnNames() {
        return insertForValColumnNames;
    }

    public List<SQL> getDeleteForWhereColumnNames() {
        return deleteForWhereColumnNames;
    }

    public List<SQL> getDeleteForWhereValColumnNames() {
        return deleteForWhereValColumnNames;
    }

    public List<SQL> getUpdateForWhereColumnNames() {
        return updateForWhereColumnNames;
    }

    public List<SQL> getUpdateForWhereValColumnNames() {
        return updateForWhereValColumnNames;
    }

    public List<SQL> getUpdateFoColumnNames() {
        return updateFoColumnNames;
    }

    public List<SQL> getUpdateForValues() {
        return updateForValues;
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

    public List<SQL> getSqlInfos() {
        return sqlInfos;
    }
}
