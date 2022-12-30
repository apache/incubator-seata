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


import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.MultiTableRecords;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The type MultiSql executor. now just support same type
 * ex.
 * <pre>
 *  jdbcTemplate.update("update account_tbl set money = money - ? where user_id = ?;update account_tbl set money = money - ? where user_id = ?", new Object[] {money, userId,"U10000",money,"U1000"});
 *  </pre>
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author wangwei.ying
 */
public class MultiExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S, MultiTableRecords> {

    private Map<String, List<SQLRecognizer>> multiSqlGroup = new HashMap<>(4);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizers    the sql recognizers
     */
    public MultiExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
    }

    /**
     * Before image table records.  only support update or deleted
     *
     * @return the table records
     * @throws SQLException the sql exception
     * @see io.seata.rm.datasource.sql.SQLVisitorFactory#get(String, String) validate sqlType
     */
    @Override
    protected MultiTableRecords beforeImage() throws SQLException {
        MultiTableRecords result = new MultiTableRecords();
        //group by sqlType
        multiSqlGroup = sqlRecognizers.stream().collect(Collectors.groupingBy(t -> t.getTableName()));
        AbstractDMLBaseExecutor<T, S, TableRecords> executor = null;
        for (List<SQLRecognizer> value : multiSqlGroup.values()) {
            switch (value.get(0).getSQLType()) {
                case UPDATE:
                    executor = new MultiUpdateExecutor<T, S>(statementProxy, statementCallback, value);
                    break;
                case DELETE:
                    executor = new MultiDeleteExecutor<T, S>(statementProxy, statementCallback, value);
                    break;
                default:
                    throw new UnsupportedOperationException("not support sql" + value.get(0).getOriginalSQL());
            }
            TableRecords beforeImage = executor.beforeImage();
            result.addTableRecords(beforeImage);
        }
        return result;
    }

    @Override
    protected MultiTableRecords afterImage(MultiTableRecords beforeImage) throws SQLException {
        MultiTableRecords result = new MultiTableRecords();
        AbstractDMLBaseExecutor<T, S, TableRecords> executor = null;
        for (List<SQLRecognizer> value : multiSqlGroup.values()) {
            switch (value.get(0).getSQLType()) {
                case UPDATE:
                    executor = new MultiUpdateExecutor<T, S>(statementProxy, statementCallback, value);
                    break;
                case DELETE:
                    executor = new MultiDeleteExecutor<T, S>(statementProxy, statementCallback, value);
                    break;
                default:
                    throw new UnsupportedOperationException("not support sql" + value.get(0).getOriginalSQL());
            }
            TableRecords tableBeforeImage = beforeImage.getTableRecordsByTableName(value.get(0).getTableName());
            TableRecords afterImage = executor.afterImage(tableBeforeImage);
            result.addTableRecords(afterImage);
        }
        return result;
    }

    public Map<String, List<SQLRecognizer>> getMultiSqlGroup() {
        return multiSqlGroup;
    }
}
