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
package org.apache.seata.saga.engine.config;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.saga.engine.StateMachineConfig;
import org.apache.seata.saga.engine.expression.ExpressionFactoryManager;
import org.apache.seata.saga.engine.expression.ExpressionResolver;
import org.apache.seata.saga.engine.expression.exception.ExceptionMatchExpressionFactory;
import org.apache.seata.saga.engine.expression.impl.DefaultExpressionResolver;
import org.apache.seata.saga.engine.expression.seq.SequenceExpressionFactory;
import org.apache.seata.saga.engine.invoker.ServiceInvokerManager;
import org.apache.seata.saga.engine.pcext.InterceptableStateHandler;
import org.apache.seata.saga.engine.pcext.InterceptableStateRouter;
import org.apache.seata.saga.engine.pcext.StateHandler;
import org.apache.seata.saga.engine.pcext.StateHandlerInterceptor;
import org.apache.seata.saga.engine.pcext.StateMachineProcessHandler;
import org.apache.seata.saga.engine.pcext.StateMachineProcessRouter;
import org.apache.seata.saga.engine.pcext.StateRouter;
import org.apache.seata.saga.engine.pcext.StateRouterInterceptor;
import org.apache.seata.saga.engine.repo.StateLogRepository;
import org.apache.seata.saga.engine.repo.StateMachineRepository;
import org.apache.seata.saga.engine.repo.impl.StateLogRepositoryImpl;
import org.apache.seata.saga.engine.repo.impl.StateMachineRepositoryImpl;
import org.apache.seata.saga.engine.sequence.SeqGenerator;
import org.apache.seata.saga.engine.sequence.UUIDSeqGenerator;
import org.apache.seata.saga.engine.store.StateLangStore;
import org.apache.seata.saga.engine.store.StateLogStore;
import org.apache.seata.saga.engine.strategy.StatusDecisionStrategy;
import org.apache.seata.saga.engine.strategy.impl.DefaultStatusDecisionStrategy;
import org.apache.seata.saga.proctrl.ProcessRouter;
import org.apache.seata.saga.proctrl.ProcessType;
import org.apache.seata.saga.proctrl.eventing.impl.AsyncEventBus;
import org.apache.seata.saga.proctrl.eventing.impl.DirectEventBus;
import org.apache.seata.saga.proctrl.eventing.impl.ProcessCtrlEventConsumer;
import org.apache.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import org.apache.seata.saga.proctrl.handler.DefaultRouterHandler;
import org.apache.seata.saga.proctrl.handler.ProcessHandler;
import org.apache.seata.saga.proctrl.handler.RouterHandler;
import org.apache.seata.saga.proctrl.impl.ProcessControllerImpl;
import org.apache.seata.saga.proctrl.process.impl.CustomizeBusinessProcessor;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.StateType;

import javax.script.ScriptEngineManager;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE;
import static org.apache.seata.common.DefaultValues.DEFAULT_SAGA_JSON_PARSER;

/**
 * Abstract StateMachineConfig
 */
public abstract class AbstractStateMachineConfig implements StateMachineConfig {

    private static final int DEFAULT_TRANS_OPERATION_TIMEOUT = 60000 * 30;
    private static final int DEFAULT_SERVICE_INVOKE_TIMEOUT = 60000 * 5;

    private ExpressionFactoryManager expressionFactoryManager;
    private ExpressionResolver expressionResolver;

    private StateLogRepository stateLogRepository;
    /**
     * NullAble
     */
    private StateLogStore stateLogStore;
    private StateMachineRepository stateMachineRepository;
    /**
     * NullAble
     */
    private StateLangStore stateLangStore;

    private StatusDecisionStrategy statusDecisionStrategy;

    private SeqGenerator seqGenerator = new UUIDSeqGenerator();

    private ProcessCtrlEventPublisher syncProcessCtrlEventPublisher;
    private ProcessCtrlEventPublisher asyncProcessCtrlEventPublisher;

    private int transOperationTimeout = DEFAULT_TRANS_OPERATION_TIMEOUT;
    private int serviceInvokeTimeout = DEFAULT_SERVICE_INVOKE_TIMEOUT;

    private boolean enableAsync = false;
    private ThreadPoolExecutor threadPoolExecutor;

    private ServiceInvokerManager serviceInvokerManager;
    private ScriptEngineManager scriptEngineManager;

    private String charset = "UTF-8";
    private String defaultTenantId = "000001";

    private String sagaJsonParser = DEFAULT_SAGA_JSON_PARSER;

    private boolean autoRegisterResources = true;

    private InputStream[] stateMachineDefInputStreamArray;

    private boolean sagaRetryPersistModeUpdate = DEFAULT_CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE;
    private boolean sagaCompensatePersistModeUpdate = DEFAULT_CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE;

    private boolean rmReportSuccessEnable = DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
    private boolean sagaBranchRegisterEnable = DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;

    public void init() throws Exception {
        // init seqGenerator
        if (seqGenerator == null) {
            seqGenerator = new UUIDSeqGenerator();
        }

        // init ExpressionFactoryManager
        if (expressionFactoryManager == null) {
            expressionFactoryManager = new ExpressionFactoryManager();

            SequenceExpressionFactory sequenceExpressionFactory = new SequenceExpressionFactory();
            sequenceExpressionFactory.setSeqGenerator(seqGenerator);
            expressionFactoryManager.putExpressionFactory(DomainConstants.EXPRESSION_TYPE_SEQUENCE, sequenceExpressionFactory);

            ExceptionMatchExpressionFactory exceptionMatchExpressionFactory = new ExceptionMatchExpressionFactory();
            expressionFactoryManager.putExpressionFactory(DomainConstants.EXPRESSION_TYPE_EXCEPTION, exceptionMatchExpressionFactory);
        }

        // init expressionResolver
        if (expressionResolver == null) {
            DefaultExpressionResolver defaultExpressionResolver = new DefaultExpressionResolver();
            defaultExpressionResolver.setExpressionFactoryManager(expressionFactoryManager);
            expressionResolver = defaultExpressionResolver;
        }

        if (stateLogRepository == null) {
            StateLogRepositoryImpl defaultStateLogRepository = new StateLogRepositoryImpl();
            defaultStateLogRepository.setStateLogStore(stateLogStore);
            this.stateLogRepository = defaultStateLogRepository;
        }

        if (stateMachineRepository == null) {
            StateMachineRepositoryImpl defaultStateMachineRepository = new StateMachineRepositoryImpl();
            defaultStateMachineRepository.setCharset(charset);
            defaultStateMachineRepository.setSeqGenerator(seqGenerator);
            defaultStateMachineRepository.setDefaultTenantId(defaultTenantId);
            defaultStateMachineRepository.setJsonParserName(sagaJsonParser);
            defaultStateMachineRepository.setStateLangStore(stateLangStore);
            this.stateMachineRepository = defaultStateMachineRepository;
        }

        // auto register resources
        if (autoRegisterResources && stateMachineDefInputStreamArray != null) {
            stateMachineRepository.registryByResources(stateMachineDefInputStreamArray, defaultTenantId);
        }

        if (statusDecisionStrategy == null) {
            statusDecisionStrategy = new DefaultStatusDecisionStrategy();
        }

        if (syncProcessCtrlEventPublisher == null) {
            ProcessCtrlEventPublisher syncEventPublisher = new ProcessCtrlEventPublisher();

            ProcessControllerImpl processorController = createProcessorController(syncEventPublisher);

            ProcessCtrlEventConsumer processCtrlEventConsumer = new ProcessCtrlEventConsumer();
            processCtrlEventConsumer.setProcessController(processorController);

            DirectEventBus directEventBus = new DirectEventBus();
            syncEventPublisher.setEventBus(directEventBus);

            directEventBus.registerEventConsumer(processCtrlEventConsumer);

            syncProcessCtrlEventPublisher = syncEventPublisher;
        }

        if (enableAsync && asyncProcessCtrlEventPublisher == null) {
            ProcessCtrlEventPublisher asyncEventPublisher = new ProcessCtrlEventPublisher();

            ProcessControllerImpl processorController = createProcessorController(asyncEventPublisher);

            ProcessCtrlEventConsumer processCtrlEventConsumer = new ProcessCtrlEventConsumer();
            processCtrlEventConsumer.setProcessController(processorController);

            AsyncEventBus asyncEventBus = new AsyncEventBus();
            asyncEventBus.setThreadPoolExecutor(getThreadPoolExecutor());
            asyncEventPublisher.setEventBus(asyncEventBus);

            asyncEventBus.registerEventConsumer(processCtrlEventConsumer);

            asyncProcessCtrlEventPublisher = asyncEventPublisher;
        }

        if (this.serviceInvokerManager == null) {
            this.serviceInvokerManager = new ServiceInvokerManager();
        }

        if (this.scriptEngineManager == null) {
            this.scriptEngineManager = new ScriptEngineManager();
        }
    }

    public ProcessControllerImpl createProcessorController(ProcessCtrlEventPublisher eventPublisher) throws Exception {
        StateMachineProcessHandler stateMachineProcessHandler = buildStateMachineProcessHandler();
        DefaultRouterHandler defaultRouterHandler = buildDefaultRouterHandler(eventPublisher);

        CustomizeBusinessProcessor customizeBusinessProcessor = new CustomizeBusinessProcessor();

        Map<String, ProcessHandler> processHandlerMap = new HashMap<>(1);
        processHandlerMap.put(ProcessType.STATE_LANG.getCode(), stateMachineProcessHandler);
        customizeBusinessProcessor.setProcessHandlers(processHandlerMap);

        Map<String, RouterHandler> routerHandlerMap = new HashMap<>(1);
        routerHandlerMap.put(ProcessType.STATE_LANG.getCode(), defaultRouterHandler);
        customizeBusinessProcessor.setRouterHandlers(routerHandlerMap);

        ProcessControllerImpl processorController = new ProcessControllerImpl();
        processorController.setBusinessProcessor(customizeBusinessProcessor);

        return processorController;
    }

    private StateMachineProcessHandler buildStateMachineProcessHandler() {
        StateMachineProcessHandler stateMachineProcessHandler = new StateMachineProcessHandler();
        stateMachineProcessHandler.initDefaultHandlers();
        loadStateHandlerInterceptors(stateMachineProcessHandler.getStateHandlers());
        return stateMachineProcessHandler;
    }

    private DefaultRouterHandler buildDefaultRouterHandler(ProcessCtrlEventPublisher eventPublisher) {
        DefaultRouterHandler defaultRouterHandler = new DefaultRouterHandler();
        defaultRouterHandler.setEventPublisher(eventPublisher);

        StateMachineProcessRouter stateMachineProcessRouter = new StateMachineProcessRouter();
        stateMachineProcessRouter.initDefaultStateRouters();
        loadStateRouterInterceptors(stateMachineProcessRouter.getStateRouters());

        Map<String, ProcessRouter> processRouterMap = new HashMap<>(2);
        processRouterMap.put(ProcessType.STATE_LANG.getCode(), stateMachineProcessRouter);
        defaultRouterHandler.setProcessRouters(processRouterMap);
        return defaultRouterHandler;
    }

    public void loadStateHandlerInterceptors(Map<StateType, StateHandler> stateHandlerMap) {
        for (StateHandler stateHandler : stateHandlerMap.values()) {
            if (stateHandler instanceof InterceptableStateHandler) {
                InterceptableStateHandler interceptableStateHandler = (InterceptableStateHandler) stateHandler;
                List<StateHandlerInterceptor> interceptorList = EnhancedServiceLoader.loadAll(StateHandlerInterceptor.class);
                for (StateHandlerInterceptor interceptor : interceptorList) {
                    if (interceptor.match(interceptableStateHandler.getClass())) {
                        interceptableStateHandler.addInterceptor(interceptor);
                    }
                }
            }
        }
    }

    public void loadStateRouterInterceptors(Map<StateType, StateRouter> stateRouterMap) {
        for (StateRouter stateRouter : stateRouterMap.values()) {
            if (stateRouter instanceof InterceptableStateRouter) {
                InterceptableStateRouter interceptableStateRouter = (InterceptableStateRouter) stateRouter;
                List<StateRouterInterceptor> interceptorList = EnhancedServiceLoader.loadAll(StateRouterInterceptor.class);
                for (StateRouterInterceptor interceptor : interceptorList) {
                    if (interceptor.match(interceptableStateRouter.getClass())) {
                        interceptableStateRouter.addInterceptor(interceptor);
                    }
                }
            }
        }
    }

    @Override
    public StateLogRepository getStateLogRepository() {
        return stateLogRepository;
    }

    @Override
    public StateLogStore getStateLogStore() {
        return stateLogStore;
    }

    @Override
    public StateLangStore getStateLangStore() {
        return stateLangStore;
    }

    @Override
    public ExpressionFactoryManager getExpressionFactoryManager() {
        return expressionFactoryManager;
    }

    @Override
    public ExpressionResolver getExpressionResolver() {
        return expressionResolver;
    }

    @Override
    public String getCharset() {
        return charset;
    }

    @Override
    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    @Override
    public StateMachineRepository getStateMachineRepository() {
        return stateMachineRepository;
    }

    @Override
    public StatusDecisionStrategy getStatusDecisionStrategy() {
        return statusDecisionStrategy;
    }

    @Override
    public SeqGenerator getSeqGenerator() {
        return seqGenerator;
    }

    @Override
    public ProcessCtrlEventPublisher getProcessCtrlEventPublisher() {
        return syncProcessCtrlEventPublisher;
    }

    @Override
    public ProcessCtrlEventPublisher getAsyncProcessCtrlEventPublisher() {
        return asyncProcessCtrlEventPublisher;
    }

    @Override
    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    @Override
    public boolean isEnableAsync() {
        return enableAsync;
    }

    @Override
    public ServiceInvokerManager getServiceInvokerManager() {
        return serviceInvokerManager;
    }

    @Override
    public int getTransOperationTimeout() {
        return transOperationTimeout;
    }

    @Override
    public int getServiceInvokeTimeout() {
        return serviceInvokeTimeout;
    }

    @Override
    public ScriptEngineManager getScriptEngineManager() {
        return scriptEngineManager;
    }

    public void setExpressionFactoryManager(ExpressionFactoryManager expressionFactoryManager) {
        this.expressionFactoryManager = expressionFactoryManager;
    }

    public void setExpressionResolver(ExpressionResolver expressionResolver) {
        this.expressionResolver = expressionResolver;
    }

    public void setStateLogRepository(StateLogRepository stateLogRepository) {
        this.stateLogRepository = stateLogRepository;
    }

    public void setStateLogStore(StateLogStore stateLogStore) {
        this.stateLogStore = stateLogStore;
    }

    public void setStateMachineRepository(StateMachineRepository stateMachineRepository) {
        this.stateMachineRepository = stateMachineRepository;
    }

    public void setStateLangStore(StateLangStore stateLangStore) {
        this.stateLangStore = stateLangStore;
    }

    public void setStatusDecisionStrategy(StatusDecisionStrategy statusDecisionStrategy) {
        this.statusDecisionStrategy = statusDecisionStrategy;
    }

    public void setSeqGenerator(SeqGenerator seqGenerator) {
        this.seqGenerator = seqGenerator;
    }

    public void setSyncProcessCtrlEventPublisher(
            ProcessCtrlEventPublisher syncProcessCtrlEventPublisher) {
        this.syncProcessCtrlEventPublisher = syncProcessCtrlEventPublisher;
    }

    public void setAsyncProcessCtrlEventPublisher(
            ProcessCtrlEventPublisher asyncProcessCtrlEventPublisher) {
        this.asyncProcessCtrlEventPublisher = asyncProcessCtrlEventPublisher;
    }

    public void setTransOperationTimeout(int transOperationTimeout) {
        this.transOperationTimeout = transOperationTimeout;
    }

    public void setServiceInvokeTimeout(int serviceInvokeTimeout) {
        this.serviceInvokeTimeout = serviceInvokeTimeout;
    }

    public void setEnableAsync(boolean enableAsync) {
        this.enableAsync = enableAsync;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public void setServiceInvokerManager(ServiceInvokerManager serviceInvokerManager) {
        this.serviceInvokerManager = serviceInvokerManager;
    }

    public void setScriptEngineManager(ScriptEngineManager scriptEngineManager) {
        this.scriptEngineManager = scriptEngineManager;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }

    public void setSagaJsonParser(String sagaJsonParser) {
        this.sagaJsonParser = sagaJsonParser;
    }

    public void setAutoRegisterResources(boolean autoRegisterResources) {
        this.autoRegisterResources = autoRegisterResources;
    }

    public void setStateMachineDefInputStreamArray(InputStream[] stateMachineDefInputStreamArray) {
        this.stateMachineDefInputStreamArray = stateMachineDefInputStreamArray;
    }

    public String getSagaJsonParser() {
        return sagaJsonParser;
    }

    public boolean isAutoRegisterResources() {
        return autoRegisterResources;
    }

    public InputStream[] getStateMachineDefInputStreamArray() {
        return stateMachineDefInputStreamArray;
    }

    public boolean isSagaRetryPersistModeUpdate() {
        return sagaRetryPersistModeUpdate;
    }

    public void setSagaRetryPersistModeUpdate(boolean sagaRetryPersistModeUpdate) {
        this.sagaRetryPersistModeUpdate = sagaRetryPersistModeUpdate;
    }

    public boolean isSagaCompensatePersistModeUpdate() {
        return sagaCompensatePersistModeUpdate;
    }

    public void setSagaCompensatePersistModeUpdate(boolean sagaCompensatePersistModeUpdate) {
        this.sagaCompensatePersistModeUpdate = sagaCompensatePersistModeUpdate;
    }

    public boolean isRmReportSuccessEnable() {
        return rmReportSuccessEnable;
    }

    public void setRmReportSuccessEnable(boolean rmReportSuccessEnable) {
        this.rmReportSuccessEnable = rmReportSuccessEnable;
    }

    public boolean isSagaBranchRegisterEnable() {
        return sagaBranchRegisterEnable;
    }

    public void setSagaBranchRegisterEnable(boolean sagaBranchRegisterEnable) {
        this.sagaBranchRegisterEnable = sagaBranchRegisterEnable;
    }
}
