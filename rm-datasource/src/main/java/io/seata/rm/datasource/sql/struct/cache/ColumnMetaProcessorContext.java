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
package io.seata.rm.datasource.sql.struct.cache;

import io.seata.sqlparser.struct.TableMeta;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * The type column meta processor context.
 *
 * @author wang.liang
 */
public class ColumnMetaProcessorContext {

    private final Connection connection;
    private final Statement statement;

    private final ResultSet resultSet;
    private final ResultSetMetaData resultSetMetaData;
    private final DatabaseMetaData databaseMetaData;

    private final TableMeta tableMeta;

    private final ResultSet rsColumns;


    public ColumnMetaProcessorContext(Connection connection, Statement statement, ResultSet resultSet,
                                      ResultSetMetaData resultSetMetaData, DatabaseMetaData databaseMetaData,
                                      TableMeta tableMeta, ResultSet rsColumns) {
        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
        this.resultSetMetaData = resultSetMetaData;
        this.databaseMetaData = databaseMetaData;
        this.tableMeta = tableMeta;
        this.rsColumns = rsColumns;
    }


    public Connection getConnection() {
        return connection;
    }

    public Statement getStatement() {
        return statement;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public ResultSetMetaData getResultSetMetaData() {
        return resultSetMetaData;
    }

    public DatabaseMetaData getDatabaseMetaData() {
        return databaseMetaData;
    }

    public TableMeta getTableMeta() {
        return tableMeta;
    }

    public ResultSet getRsColumns() {
        return rsColumns;
    }

}
