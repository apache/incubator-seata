/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.exec.dm;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.BaseInsertExecutor;
import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.struct.Sequenceable;
import org.apache.seata.sqlparser.struct.SqlMethodExpr;
import org.apache.seata.sqlparser.struct.SqlSequenceExpr;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The type DM insert executor.
 *
 */
@LoadLevel(name = JdbcConstants.DM, scope = Scope.PROTOTYPE)
public class DmInsertExecutor extends BaseInsertExecutor implements Sequenceable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmInsertExecutor.class);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public DmInsertExecutor(StatementProxy statementProxy, StatementCallback statementCallback,
                            SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public Map<String,List<Object>> getPkValues() throws SQLException {
        Map<String,List<Object>> pkValuesMap = null;
        Boolean isContainsPk = containsPK();
        //when there is only one pk in the table
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
        return pkValuesMap;
    }

    @Override
    public Map<String,List<Object>> getPkValuesByColumn() throws SQLException {
        Map<String,List<Object>> pkValuesMap  = parsePkValuesFromStatement();
        String pkKey = pkValuesMap.keySet().iterator().next();
        List<Object> pkValues = pkValuesMap.get(pkKey);

        if (!pkValues.isEmpty() && pkValues.get(0) instanceof SqlSequenceExpr) {
            pkValuesMap.put(pkKey,getPkValuesBySequence((SqlSequenceExpr) pkValues.get(0)));
        } else if (pkValues.size() == 1 && pkValues.get(0) instanceof SqlMethodExpr) {
            pkValuesMap.put(pkKey,getGeneratedKeys());
        } else if (pkValues.size() == 1 && pkValues.get(0) instanceof Null) {
            throw new NotSupportYetException("dm not support null");
        }

        return pkValuesMap;
    }

    @Override
    public String getSequenceSql(SqlSequenceExpr expr) {
        return "SELECT " + expr.getSequence() + ".currval";
    }
}
