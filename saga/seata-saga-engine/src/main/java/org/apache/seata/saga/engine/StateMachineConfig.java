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
package org.apache.seata.saga.engine;

import org.apache.seata.saga.engine.expression.ExpressionFactoryManager;
import org.apache.seata.saga.engine.expression.ExpressionResolver;
import org.apache.seata.saga.engine.invoker.ServiceInvokerManager;
import org.apache.seata.saga.engine.repo.StateLogRepository;
import org.apache.seata.saga.engine.repo.StateMachineRepository;
import org.apache.seata.saga.engine.sequence.SeqGenerator;
import org.apache.seata.saga.engine.store.StateLangStore;
import org.apache.seata.saga.engine.store.StateLogStore;
import org.apache.seata.saga.engine.strategy.StatusDecisionStrategy;
import org.apache.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;

import javax.script.ScriptEngineManager;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * StateMachineConfig
 *
 */
public interface StateMachineConfig {

    /**
     * Gets state log store.
     *
     * @return the StateLogRepository
     */
    StateLogRepository getStateLogRepository();

    /**
     * Gets get state log store.
     *
     * @return the get StateLogStore
     */
    StateLogStore getStateLogStore();

    /**
     * Gets get state language definition store.
     *
     * @return the get StateLangStore
     */
    StateLangStore getStateLangStore();

    /**
     * Gets get expression factory manager.
     *
     * @return the get expression factory manager
     */
    ExpressionFactoryManager getExpressionFactoryManager();

    /**
     * Gets get expression resolver
     *
     * @return the get expression resolver
     */
    ExpressionResolver getExpressionResolver();

    /**
     * Gets get charset.
     *
     * @return the get charset
     */
    String getCharset();

    /**
     * Gets get default tenant id.
     *
     * @return the default tenant id
     */
    String getDefaultTenantId();

    /**
     * Gets get state machine repository.
     *
     * @return the get state machine repository
     */
    StateMachineRepository getStateMachineRepository();

    /**
     * Gets get status decision strategy.
     *
     * @return the get status decision strategy
     */
    StatusDecisionStrategy getStatusDecisionStrategy();

    /**
     * Gets get seq generator.
     *
     * @return the get seq generator
     */
    SeqGenerator getSeqGenerator();

    /**
     * Gets get process ctrl event publisher.
     *
     * @return the get process ctrl event publisher
     */
    ProcessCtrlEventPublisher getProcessCtrlEventPublisher();

    /**
     * Gets get async process ctrl event publisher.
     *
     * @return the get async process ctrl event publisher
     */
    ProcessCtrlEventPublisher getAsyncProcessCtrlEventPublisher();

    /**
     * Gets get thread pool executor.
     *
     * @return the get thread pool executor
     */
    ThreadPoolExecutor getThreadPoolExecutor();

    /**
     * Is enable async boolean.
     *
     * @return the boolean
     */
    boolean isEnableAsync();

    /**
     * get ServiceInvokerManager
     *
     * @return the service invoker manager info
     */
    ServiceInvokerManager getServiceInvokerManager();

    /**
     * get trans operation timeout
     * @return the transaction operate time out
     */
    int getTransOperationTimeout();

    /**
     * get service invoke timeout
     * @return the service invoke time out
     */
    int getServiceInvokeTimeout();

    /**
     * get ScriptEngineManager
     *
     * @return the script engine manager info
     */
    ScriptEngineManager getScriptEngineManager();
}
