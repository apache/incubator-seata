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
package io.seata.saga.engine;

import java.util.concurrent.ThreadPoolExecutor;

import io.seata.saga.engine.evaluation.EvaluatorFactoryManager;
import io.seata.saga.engine.expression.ExpressionFactoryManager;
import io.seata.saga.engine.invoker.ServiceInvokerManager;
import io.seata.saga.engine.repo.StateLogRepository;
import io.seata.saga.engine.repo.StateMachineRepository;
import io.seata.saga.engine.sequence.SeqGenerator;
import io.seata.saga.engine.store.StateLangStore;
import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.engine.strategy.StatusDecisionStrategy;
import io.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import org.springframework.context.ApplicationContext;

/**
 * StateMachineConfig
 *
 * @author lorne.cl
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
     * Gets get evaluator factory manager.
     *
     * @return the get evaluator factory manager
     */
    EvaluatorFactoryManager getEvaluatorFactoryManager();

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
     * Gets get application context.
     *
     * @return the get application context
     */
    ApplicationContext getApplicationContext();

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
     * @return
     */
    ServiceInvokerManager getServiceInvokerManager();
}