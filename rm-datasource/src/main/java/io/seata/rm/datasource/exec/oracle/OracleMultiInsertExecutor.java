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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.AbstractDMLBaseExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;


/**
 * The type oracle multi insert recognizer.
 *
 * @author renliangyu857
 */
public class OracleMultiInsertExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {
    /**
     * Instantiates a new Multi update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizers    the sql recognizers
     */
    public OracleMultiInsertExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        if (CollectionUtils.isEmpty(sqlRecognizers)) {
            return null;
        } else {
            return TableRecords.empty(getTableMeta(sqlRecognizers.get(0).getTableName()));
        }
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        if (CollectionUtils.isEmpty(sqlRecognizers)) {
            return null;
        }
        TableRecords tableRecords = new TableRecords(getTableMeta(sqlRecognizers.get(0).getTableName()));
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            OracleInsertExecutor executor = new OracleInsertExecutor(statementProxy, statementCallback, sqlRecognizer);
            TableRecords itemRecord = executor.afterImage(beforeImage);
            tableRecords.addRows(itemRecord.getRows());
        }
        return tableRecords;
    }
}
