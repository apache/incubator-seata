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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.exec.constant.SQLTypeConstant;
import io.seata.rm.datasource.exec.handler.AfterHandler;
import io.seata.rm.datasource.exec.handler.AfterHandlerFactory;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author: yangyicong
 */
@LoadLevel(name = JdbcConstants.MYSQL, scope = Scope.PROTOTYPE)
public class MySQLInsertOrUpdateExecutor extends MySQLInsertExecutor implements Defaultable {

    /**
     * before image sql and after image sql,condition is unique index
     */
    private String selectSQL;

    private AfterHandler afterHandler;

    public String getSelectSQL() {
        return selectSQL;
    }

    public void setSelectSQL(String selectSQL) {
        this.selectSQL = selectSQL;
    }

    public MySQLInsertOrUpdateExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
        afterHandler = AfterHandlerFactory.getAfterHandler(SQLTypeConstant.INSERT_ON_DUPLICATE_UPDATE);
    }

    @Override
    public TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        TableMeta tmeta = getTableMeta();
        String afterSelectSQL = afterHandler.buildAfterSelectSQL(beforeImage);
        return buildTableRecords2(tmeta, selectSQL + afterSelectSQL, new ArrayList<>(paramAppenderMap.values()));
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tmeta = getTableMeta();
        //after image sql the same of before image
        if (StringUtils.isBlank(selectSQL)) {
            selectSQL = buildImageSQL(tmeta);
        }
        if (CollectionUtils.isEmpty(paramAppenderMap)) {
            throw new ShouldNeverHappenException("can not find unique param,may be you should add unique key when use the sqlType of " +
                    "on duplicate key update ");
        }
        return buildTableRecords2(tmeta, selectSQL, new ArrayList<>(paramAppenderMap.values()));
    }


    /**
     * build sql params
     *
     * @param recognizer SQLInsertRecognizer
     * @return map, key is column, value is paramperter
     */
    @Override
    @SuppressWarnings("lgtm[java/dereferenced-value-may-be-null]")
    public Map<String, ArrayList<Object>> buildImageParamperters(SQLInsertRecognizer recognizer) {
        List<String> duplicateKeyUpdateCloms = recognizer.getDuplicateKeyUpdate();
        if (CollectionUtils.isNotEmpty(duplicateKeyUpdateCloms)) {
            getTableMeta().getAllIndexes().forEach((k, v) -> {
                if ("PRIMARY".equalsIgnoreCase(k)) {
                    for (ColumnMeta m : v.getValues()) {
                        if (duplicateKeyUpdateCloms.contains(m.getColumnName())) {
                            throw new ShouldNeverHappenException("update pk value is not supported!");
                        }
                    }
                }
            });
        }
        return super.buildImageParamperters(recognizer);
    }

    @Override
    public Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage) {
        return afterHandler.buildUndoRow(beforeImage, afterImage);
    }

}
