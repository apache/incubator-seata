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
import org.mybatis.spring.SqlSessionTemplate;

import static io.seata.saga.engine.store.db.MybatisConfig.MAPPER_PREFIX;

/**
 * State language definition store in DB
 *
 * @author lorne.cl
 */
public class DBStateLangStore implements StateLangStore {

    private SqlSessionTemplate sqlSessionTemplate;

    @Override
    public StateMachine getStateMachineById(String stateMachineId) {
        return sqlSessionTemplate.selectOne( MAPPER_PREFIX + "getStateMachineById", stateMachineId);
    }

    @Override
    public StateMachine getLastVersionStateMachine(String stateMachineName) {
        return sqlSessionTemplate.selectOne( MAPPER_PREFIX + "getLastVersionStateMachine", stateMachineName);
    }

    @Override
    public boolean storeStateMachine(StateMachine stateMachine) {
        return sqlSessionTemplate.insert(MAPPER_PREFIX + "insertStateMachine", stateMachine) > 0;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }
}