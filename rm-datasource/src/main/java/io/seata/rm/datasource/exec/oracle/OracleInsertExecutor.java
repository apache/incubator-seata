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
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.BaseInsertExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.Sequenceable;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jsbxyyx
 */
@LoadLevel(name = JdbcConstants.ORACLE, scope = Scope.PROTOTYPE)
public class OracleInsertExecutor extends BaseInsertExecutor implements Sequenceable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleInsertExecutor.class);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public OracleInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                                SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public Map<String,List<Object>> getPkValues() throws SQLException {
        Map<String,List<Object>> pkValuesMap = null;
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
        Boolean isContainsPk = containsPK();
        //when there is only one pk in the table
        if (getTableMeta().getPrimaryKeyOnlyName().size() == 1) {
            if (isContainsPk) {
                pkValuesMap = getPkValuesByColumn();
            }
            else if (containsColumns()) {
                String columnName = getTableMeta().getPrimaryKeyOnlyName().get(0);
                pkValuesMap = Collections.singletonMap(columnName, getGeneratedKeys());
            }
            else {
                pkValuesMap = getPkValuesByColumn();
            }
        } else {
            //when there is multiple pk in the table
            //1,all pk columns are filled value.
            //2,the auto increment pk column value is null, and other pk value are not null.
            pkValuesMap = getPkValuesByColumn();
            for (String columnName:pkColumnNameList) {
                if (!pkValuesMap.containsKey(columnName)) {
                    ColumnMeta pkColumnMeta = getTableMeta().getColumnMeta(columnName);
                    if (Objects.nonNull(pkColumnMeta)) {
                        //3,the auto increment pk column is not exits in sql, and other pk are exits also the value is not null.
                        pkValuesMap.put(pkColumnMeta.getColumnName(),getGeneratedKeys());
                    }
                }
            }
        }
        return pkValuesMap;
    }

    @Override
    public Map<String,List<Object>> getPkValuesByColumn() throws SQLException {
        Map<String,List<Object>> pkValuesMap  = parsePkValuesFromStatement();
        Set<String> keySet = new HashSet<>(pkValuesMap.keySet());
        //auto increment
        for (String pkKey:keySet) {
            List<Object> pkValues = pkValuesMap.get(pkKey);
            boolean b = this.checkPkValues(pkValues);
            if (!b) {
                throw new NotSupportYetException("not support sql [" + sqlRecognizer.getOriginalSQL() + "]");
            }

            if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlSequenceExpr) {
                pkValuesMap.put(pkKey,getPkValuesBySequence((SqlSequenceExpr) pkValues.get(0)));
            } else if (pkValues.size() == 1 && pkValues.get(0) instanceof SqlMethodExpr) {
                pkValuesMap.put(pkKey,getGeneratedKeys());
            } else if (pkValues.size() == 1 && pkValues.get(0) instanceof Null) {
                throw new NotSupportYetException("oracle not support null");
            }
        }

        return pkValuesMap;
    }

    @Override
    public String getSequenceSql(SqlSequenceExpr expr) {
        return "SELECT " + expr.getSequence() + ".currval FROM DUAL";
    }
}
