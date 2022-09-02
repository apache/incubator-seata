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
package io.seata.rm.datasource.exec.mysql;

import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.exec.constant.SQLTypeConstant;
import io.seata.rm.datasource.exec.handler.AfterHandler;
import io.seata.rm.datasource.exec.handler.AfterHandlerFactory;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * @author: lyx
 */
@LoadLevel(name = JdbcConstants.MYSQL, scope = Scope.PROTOTYPE)
public class MySQLInsertIgnoreExecutor extends MySQLInsertExecutor implements Defaultable {

    /**
     * before image sql and after image sql,condition is unique index
     */
    private String selectSQL;

    private AfterHandler afterHandler;

    public void setSelectSQL(String selectSQL) {
        this.selectSQL = selectSQL;
    }

    public MySQLInsertIgnoreExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
        afterHandler = AfterHandlerFactory.getAfterHandler(SQLTypeConstant.INSERT_IGNORE);
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tableMeta = getTableMeta();
        // after image sql the same of before image
        if (StringUtils.isBlank(selectSQL)) {
            selectSQL = buildImageSQL(tableMeta);
        }
        if (CollectionUtils.isEmpty(paramAppenderMap)) {
            return TableRecords.empty(tableMeta);
        }
        return buildTableRecords2(tableMeta, selectSQL, new ArrayList<List<Object>>(paramAppenderMap.values()));
    }

    @Override
    public TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        // just insert without no uk
        if (CollectionUtils.isEmpty(paramAppenderMap)) {
            return super.afterImage(beforeImage);
        }
        TableMeta tableMeta = getTableMeta();
        return buildTableRecords2(tableMeta, selectSQL + afterHandler.buildAfterSelectSQL(beforeImage),
                new ArrayList<List<Object>>(paramAppenderMap.values()));
    }

    @Override
    public Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage) {
        return afterHandler.buildUndoRow(beforeImage, afterImage);
    }
}
