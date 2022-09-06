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
package io.seata.rm.datasource.exec;


import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.oceanbaseoracle.OceanBaseOracleMultiInsertExecutor;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Multi operations executor
 * NOTE: Only multiple operations of the same type are supported for now
 *
 * @author wangwei.ying
 * @author hsien999
 */
public class MultiExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    private final Map<String, List<SQLRecognizer>> multiSqlGroup;
    private final Map<SQLRecognizer, TableRecords> beforeImagesMap;
    private final Map<SQLRecognizer, TableRecords> afterImagesMap;

    public MultiExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
        multiSqlGroup = sqlRecognizers.stream().collect(Collectors.groupingBy(SQLRecognizer::getTableName));
        beforeImagesMap = new HashMap<>(3, 1.f);
        afterImagesMap = new HashMap<>(3, 1.f);
    }

    /**
     * Unlike a single executor, this function uses {@link #beforeImagesMap}
     * to associate different table sources with the before image records, which is used to prepare undo log.
     *
     * @return always returns null
     * @throws SQLException the sql exception
     */
    @Override
    protected TableRecords beforeImage() throws SQLException {
        AbstractDMLBaseExecutor<T, S> executor;
        for (List<SQLRecognizer> recognizers : multiSqlGroup.values()) {
            executor = getExecutor(recognizers);
            TableRecords beforeImage = executor.beforeImage();
            beforeImagesMap.put(recognizers.get(0), beforeImage);
        }
        return null;
    }

    /**
     * As with {@link #beforeImage()}, this function uses {@link #beforeImagesMap} and {@link #afterImagesMap}
     * to associate different table sources with the before image records, which is used to prepare undo log.
     *
     * @param beforeImage the before image (accepts null)
     * @return always returns null
     * @throws SQLException the sql exception
     * @see #beforeImage()
     */
    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        AbstractDMLBaseExecutor<T, S> executor;
        for (List<SQLRecognizer> recognizers : multiSqlGroup.values()) {
            executor = getExecutor(recognizers);
            beforeImage = beforeImagesMap.get(recognizers.get(0));
            TableRecords afterImage = executor.afterImage(beforeImage);
            afterImagesMap.put(recognizers.get(0), afterImage);
        }
        return null;
    }

    /**
     * Function that adds undo log to the context of the current connection based on the before image in
     * {@link #beforeImagesMap} and the after image in {@link #afterImagesMap} of the different table sources.
     *
     * @param beforeImage the before image(accepts null)
     * @param afterImage  the after image(accepts null)
     * @throws SQLException the sql exception
     */
    @Override
    protected void prepareUndoLog(TableRecords beforeImage, TableRecords afterImage) throws SQLException {
        SQLRecognizer recognizer;
        for (Map.Entry<SQLRecognizer, TableRecords> entry : beforeImagesMap.entrySet()) {
            sqlRecognizer = recognizer = entry.getKey();
            beforeImage = entry.getValue();
            afterImage = afterImagesMap.get(recognizer);
            super.prepareUndoLog(beforeImage, afterImage);
        }
    }

    public Map<String, List<SQLRecognizer>> getMultiSqlGroup() {
        return multiSqlGroup;
    }

    public Map<SQLRecognizer, TableRecords> getBeforeImagesMap() {
        return beforeImagesMap;
    }

    public Map<SQLRecognizer, TableRecords> getAfterImagesMap() {
        return afterImagesMap;
    }

    private AbstractDMLBaseExecutor<T, S> getExecutor(List<SQLRecognizer> recognizers) {
        SQLRecognizer recognizer0 = recognizers.get(0);
        switch (recognizer0.getSQLType()) {
            case UPDATE:
                return new MultiUpdateExecutor<>(statementProxy, statementCallback, recognizers);
            case DELETE:
                return new MultiDeleteExecutor<>(statementProxy, statementCallback, recognizers);
            case INSERT: {
                if (JdbcConstants.OCEANBASE_ORACLE.equals(statementProxy.getConnectionProxy().getDbType())) {
                    return new OceanBaseOracleMultiInsertExecutor<>(statementProxy, statementCallback, recognizers);
                }
            }
            default:
                throw new NotSupportYetException("Not supported sql: " + recognizer0.getOriginalSQL());
        }
    }
}
