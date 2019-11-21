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
package io.seata.saga.engine.store.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import io.seata.saga.engine.store.StateLangStore;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateMachine.Status;
import io.seata.saga.statelang.domain.impl.StateMachineImpl;

/**
 * State language definition store in DB
 *
 * @author lorne.cl
 */
public class DbStateLangStore extends AbstractStore implements StateLangStore {

    private static final ResultSetToStateMachine RESULT_SET_TO_STATE_MACHINE = new ResultSetToStateMachine();

    private static final StateMachineToStatement STATE_MACHINE_TO_STATEMENT = new StateMachineToStatement();

    private StateLangStoreSqls stateLangStoreSqls;

    @Override
    public StateMachine getStateMachineById(String stateMachineId) {
        return selectOne(stateLangStoreSqls.getGetStateMachineByIdSql(dbType), RESULT_SET_TO_STATE_MACHINE,
            stateMachineId);
    }

    @Override
    public StateMachine getLastVersionStateMachine(String stateMachineName, String tenantId) {

        List<StateMachine> list = selectList(stateLangStoreSqls.getQueryStateMachinesByNameAndTenantSql(dbType),
            RESULT_SET_TO_STATE_MACHINE, stateMachineName, tenantId);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public boolean storeStateMachine(StateMachine stateMachine) {
        return executeUpdate(stateLangStoreSqls.getInsertStateMachineSql(dbType), STATE_MACHINE_TO_STATEMENT,
            stateMachine) > 0;
    }

    @Override
    public void setTablePrefix(String tablePrefix) {
        super.setTablePrefix(tablePrefix);
        this.stateLangStoreSqls = new StateLangStoreSqls(tablePrefix);
    }

    private static class ResultSetToStateMachine implements ResultSetToObject<StateMachine> {
        @Override
        public StateMachine toObject(ResultSet resultSet) throws SQLException {
            StateMachineImpl stateMachine = new StateMachineImpl();
            stateMachine.setId(resultSet.getString("id"));
            stateMachine.setName(resultSet.getString("name"));
            stateMachine.setComment(resultSet.getString("comment_"));
            stateMachine.setVersion(resultSet.getString("ver"));
            stateMachine.setAppName(resultSet.getString("app_name"));
            stateMachine.setContent(resultSet.getString("content"));
            stateMachine.setGmtCreate(resultSet.getTimestamp("gmt_create"));
            stateMachine.setType(resultSet.getString("type"));
            stateMachine.setRecoverStrategy(resultSet.getString("recover_strategy"));
            stateMachine.setTenantId(resultSet.getString("tenant_id"));
            stateMachine.setStatus(Status.valueOf(resultSet.getString("status")));
            return stateMachine;
        }
    }

    private static class StateMachineToStatement implements ObjectToStatement<StateMachine> {
        @Override
        public void toStatement(StateMachine stateMachine, PreparedStatement statement) throws SQLException {
            statement.setString(1, stateMachine.getId());
            statement.setString(2, stateMachine.getTenantId());
            statement.setString(3, stateMachine.getAppName());
            statement.setString(4, stateMachine.getName());
            statement.setString(5, stateMachine.getStatus().name());
            statement.setTimestamp(6, new Timestamp(stateMachine.getGmtCreate().getTime()));
            statement.setString(7, stateMachine.getVersion());
            statement.setString(8, stateMachine.getType());
            statement.setString(9, stateMachine.getContent());
            statement.setString(10, stateMachine.getRecoverStrategy());
            statement.setString(11, stateMachine.getComment());
        }
    }
}