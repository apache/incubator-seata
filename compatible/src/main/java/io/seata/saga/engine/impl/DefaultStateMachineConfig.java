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
package io.seata.saga.engine.impl;

import io.seata.saga.engine.StateMachineConfig;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.script.ScriptEngineManager;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Default state machine configuration
 *
 */
public class DefaultStateMachineConfig implements StateMachineConfig, ApplicationContextAware, InitializingBean {

    private final org.apache.seata.saga.engine.impl.DefaultStateMachineConfig actual;

    public DefaultStateMachineConfig(org.apache.seata.saga.engine.impl.DefaultStateMachineConfig actual) {
        this.actual = actual;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        actual.afterPropertiesSet();
    }

    @Override
    public StateLogStore getStateLogStore() {
        return actual.getStateLogStore();
    }

    public void setStateLogStore(StateLogStore stateLogStore) {
        actual.setStateLogStore(stateLogStore);
    }

    @Override
    public StateLangStore getStateLangStore() {
        return actual.getStateLangStore();
    }

    public void setStateLangStore(StateLangStore stateLangStore) {
        actual.setStateLangStore(stateLangStore);
    }

    @Override
    public ExpressionFactoryManager getExpressionFactoryManager() {
        return actual.getExpressionFactoryManager();
    }

    public void setExpressionFactoryManager(ExpressionFactoryManager expressionFactoryManager) {
        actual.setExpressionFactoryManager(expressionFactoryManager);
    }

    @Override
    public ExpressionResolver getExpressionResolver() {
        return actual.getExpressionResolver();
    }

    public void setExpressionResolver(ExpressionResolver expressionResolver) {
        actual.setExpressionResolver(expressionResolver);
    }

    @Override
    public String getCharset() {
        return actual.getCharset();
    }

    public void setCharset(String charset) {
        actual.setCharset(charset);
    }

    @Override
    public StateMachineRepository getStateMachineRepository() {
        return actual.getStateMachineRepository();
    }

    public void setStateMachineRepository(StateMachineRepository stateMachineRepository) {
        actual.setStateMachineRepository(stateMachineRepository);
    }

    @Override
    public StatusDecisionStrategy getStatusDecisionStrategy() {
        return actual.getStatusDecisionStrategy();
    }

    public void setStatusDecisionStrategy(StatusDecisionStrategy statusDecisionStrategy) {
        actual.setStatusDecisionStrategy(statusDecisionStrategy);
    }

    @SuppressWarnings("lgtm[java/unsafe-double-checked-locking]")
    @Override
    public SeqGenerator getSeqGenerator() {
        return actual.getSeqGenerator();
    }

    public void setSeqGenerator(SeqGenerator seqGenerator) {
        actual.setSeqGenerator(seqGenerator);
    }

    @Override
    public ProcessCtrlEventPublisher getProcessCtrlEventPublisher() {
        return actual.getProcessCtrlEventPublisher();
    }

    @Override
    public ProcessCtrlEventPublisher getAsyncProcessCtrlEventPublisher() {
        return actual.getAsyncProcessCtrlEventPublisher();
    }

    public void setAsyncProcessCtrlEventPublisher(ProcessCtrlEventPublisher asyncProcessCtrlEventPublisher) {
        actual.setAsyncProcessCtrlEventPublisher(asyncProcessCtrlEventPublisher);
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return actual.getApplicationContext();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        actual.setApplicationContext(applicationContext);
    }

    @Override
    public ThreadPoolExecutor getThreadPoolExecutor() {
        return actual.getThreadPoolExecutor();
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        actual.setThreadPoolExecutor(threadPoolExecutor);
    }

    @Override
    public boolean isEnableAsync() {
        return actual.isEnableAsync();
    }

    public void setEnableAsync(boolean enableAsync) {
        actual.setEnableAsync(enableAsync);
    }

    @Override
    public StateLogRepository getStateLogRepository() {
        return actual.getStateLogRepository();
    }

    public void setStateLogRepository(StateLogRepository stateLogRepository) {
        actual.setStateLogRepository(stateLogRepository);
    }

    public void setSyncProcessCtrlEventPublisher(ProcessCtrlEventPublisher syncProcessCtrlEventPublisher) {
        actual.setSyncProcessCtrlEventPublisher(syncProcessCtrlEventPublisher);
    }

    public void setAutoRegisterResources(boolean autoRegisterResources) {
        actual.setAutoRegisterResources(autoRegisterResources);
    }

    public void setResources(String[] resources) {
        actual.setResources(resources);
    }

    @Override
    public ServiceInvokerManager getServiceInvokerManager() {
        return actual.getServiceInvokerManager();
    }

    public void setServiceInvokerManager(ServiceInvokerManager serviceInvokerManager) {
        actual.setServiceInvokerManager(serviceInvokerManager);
    }

    @Override
    public String getDefaultTenantId() {
        return actual.getDefaultTenantId();
    }

    public void setDefaultTenantId(String defaultTenantId) {
        actual.setDefaultTenantId(defaultTenantId);
    }

    @Override
    public int getTransOperationTimeout() {
        return actual.getTransOperationTimeout();
    }

    public void setTransOperationTimeout(int transOperationTimeout) {
        actual.setTransOperationTimeout(transOperationTimeout);
    }

    @Override
    public int getServiceInvokeTimeout() {
        return actual.getServiceInvokeTimeout();
    }

    public void setServiceInvokeTimeout(int serviceInvokeTimeout) {
        actual.setServiceInvokeTimeout(serviceInvokeTimeout);
    }

    @Override
    public ScriptEngineManager getScriptEngineManager() {
        return actual.getScriptEngineManager();
    }

    public void setScriptEngineManager(ScriptEngineManager scriptEngineManager) {
        actual.setScriptEngineManager(scriptEngineManager);
    }

    public String getSagaJsonParser() {
        return actual.getSagaJsonParser();
    }

    public void setSagaJsonParser(String sagaJsonParser) {
        actual.setSagaJsonParser(sagaJsonParser);
    }

    public boolean isSagaRetryPersistModeUpdate() {
        return actual.isSagaRetryPersistModeUpdate();
    }

    public void setSagaRetryPersistModeUpdate(boolean sagaRetryPersistModeUpdate) {
        actual.setSagaRetryPersistModeUpdate(sagaRetryPersistModeUpdate);
    }

    public boolean isSagaCompensatePersistModeUpdate() {
        return actual.isSagaCompensatePersistModeUpdate();
    }

    public void setSagaCompensatePersistModeUpdate(boolean sagaCompensatePersistModeUpdate) {
        actual.setSagaCompensatePersistModeUpdate(sagaCompensatePersistModeUpdate);
    }

    public static DefaultStateMachineConfig wrap(org.apache.seata.saga.engine.impl.DefaultStateMachineConfig target) {
        return new DefaultStateMachineConfig(target);
    }

    public org.apache.seata.saga.engine.impl.DefaultStateMachineConfig unwrap() {
        return actual;
    }
}
