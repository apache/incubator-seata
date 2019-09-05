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

import io.seata.saga.engine.store.StateLangStore;
import io.seata.saga.statelang.domain.StateMachine;

import java.util.HashMap;
import java.util.Map;

import static io.seata.saga.engine.store.db.MybatisConfig.MAPPER_PREFIX;

/**
 * State language definition store in DB
 *
 * @author lorne.cl
 */
public class DBStateLangStore implements StateLangStore {

    private SqlSessionExecutor sqlSessionExecutor;

    @Override
    public StateMachine getStateMachineById(String stateMachineId) {
        return sqlSessionExecutor.selectOne( MAPPER_PREFIX + "getStateMachineById", stateMachineId);
    }

    @Override
    public StateMachine getLastVersionStateMachine(String stateMachineName, String tenantId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("name", stateMachineName);
        params.put("tenantId", tenantId);
        return sqlSessionExecutor.selectOne( MAPPER_PREFIX + "getLastVersionStateMachine", params);
    }

    @Override
    public boolean storeStateMachine(StateMachine stateMachine) {
        return sqlSessionExecutor.insert(MAPPER_PREFIX + "insertStateMachine", stateMachine) > 0;
    }

    public void setSqlSessionExecutor(SqlSessionExecutor sqlSessionExecutor) {
        this.sqlSessionExecutor = sqlSessionExecutor;
    }
}