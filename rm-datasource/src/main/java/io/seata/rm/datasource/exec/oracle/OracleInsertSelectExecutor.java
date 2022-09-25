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
package io.seata.rm.datasource.exec.oracle;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.exec.constant.SQLTypeConstant;
import io.seata.rm.datasource.exec.handler.AfterHandler;
import io.seata.rm.datasource.exec.handler.AfterHandlerFactory;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: lyx
 */
public class OracleInsertSelectExecutor extends OracleInsertExecutor {

    /**
     * insert recognizer from sql
     */
    private SQLInsertRecognizer insertRecognizer;

    private String selectSQL;

    private AfterHandler afterHandler;

    public OracleInsertSelectExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) throws SQLException {
        super(statementProxy, statementCallback, sqlRecognizer);
        createInsertRecognizer();
    }

    public void createInsertRecognizer() throws SQLException {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        // get the sql after insert
        String querySQL = recognizer.getQuerySQL();
        insertRecognizer = doCreateInsertRecognizer(querySQL);
        afterHandler = prepareAfterHandler();
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tableMeta = getTableMeta();
        // after image sql the same of before image
        if (io.seata.common.util.StringUtils.isBlank(selectSQL)) {
            selectSQL = buildImageSQL(tableMeta);
        }
        // oracle insert select not support to get pk from drive,so should contain one of unique key
        if (CollectionUtils.isEmpty(paramAppenderMap)) {
            throw new NotSupportYetException("please make sure what you insert contain unique key " +
                    "in the IGNORE_ROW_ON_DUPKEY_INDEX hint");
        }
        if (Objects.isNull(afterHandler)) {
            return TableRecords.empty(tableMeta);
        } else {
            return buildTableRecords2(tableMeta, selectSQL, new ArrayList<List<Object>>(paramAppenderMap.values()));
        }
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        return buildTableRecords2(getTableMeta(), selectSQL, new ArrayList<List<Object>>(paramAppenderMap.values()));
    }

    @Override
    protected Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage) {
        return afterHandler.buildUndoRow(beforeImage, afterImage);
    }

    /**
     * from the sql type to get insert params values
     * unless select insert,other in {@link SQLInsertRecognizer#getInsertParamsValue}
     *
     * @return the insert params values
     */
    @Override
    public List<String> getInsertParamsValue() {
        return Objects.nonNull(insertRecognizer) ? insertRecognizer.getInsertParamsValue() : Collections.emptyList();
    }

    /**
     * from the sql type to get insert rows
     * unless select insert,other in {@link SQLInsertRecognizer#getInsertRows(Collection)}
     *
     * @param primaryKeyIndex the primary key index
     * @return the insert rows
     */
    @Override
    public List<List<Object>> getInsertRows(Collection primaryKeyIndex) {
        return Objects.nonNull(insertRecognizer) ? insertRecognizer.getInsertRows(primaryKeyIndex) : Collections.emptyList();
    }

    private AfterHandler prepareAfterHandler() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        return recognizer.isIgnore() ? AfterHandlerFactory.getAfterHandler(SQLTypeConstant.INSERT_IGNORE) : null;
    }

    @Override
    public Map<String, ArrayList<Object>> buildImageParameters(SQLInsertRecognizer recognizer) {
        List<String> insertParamsList = getInsertParamsValue();
        List<String> insertColumns = Optional.ofNullable(recognizer.getInsertColumns()).map(list -> list.stream()
                .map(column -> ColumnUtils.delEscape(column, getDbType())).collect(Collectors.toList())).orElse(null);
        if (CollectionUtils.isEmpty(insertColumns)) {
            insertColumns = getTableMeta(recognizer.getTableName()).getDefaultTableColumn();
        }
        Map<String, ArrayList<Object>> imageParamperterMap = new HashMap<>(insertColumns.size(), 1);
        for (String insertParams : insertParamsList) {
            String[] insertParamsArray = insertParams.split(",");
            for (int i = 0; i < insertColumns.size(); i++) {
                String m = ColumnUtils.delEscape(insertColumns.get(i), getDbType());
                String params = insertParamsArray[i];
                ArrayList<Object> imageListTemp = imageParamperterMap.computeIfAbsent(m, k -> new ArrayList<>());
                imageListTemp.add(params.trim());
                imageParamperterMap.put(m, imageListTemp);
            }
        }
        return imageParamperterMap;
    }

}

