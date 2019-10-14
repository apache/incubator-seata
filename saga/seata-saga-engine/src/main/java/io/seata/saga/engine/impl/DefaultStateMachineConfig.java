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
package io.seata.saga.engine.impl;

import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.evaluation.EvaluatorFactoryManager;
import io.seata.saga.engine.evaluation.exception.ExceptionMatchEvaluatorFactory;
import io.seata.saga.engine.evaluation.expression.ExpressionEvaluatorFactory;
import io.seata.saga.engine.expression.ExpressionFactoryManager;
import io.seata.saga.engine.expression.seq.SequenceExpressionFactory;
import io.seata.saga.engine.expression.spel.SpringELExpressionFactory;
import io.seata.saga.engine.invoker.ServiceInvokerManager;
import io.seata.saga.engine.pcext.StateMachineProcessHandler;
import io.seata.saga.engine.pcext.StateMachineProcessRouter;
import io.seata.saga.engine.store.StateLangStore;
import io.seata.saga.engine.StatusDecisionStrategy;
import io.seata.saga.proctrl.ProcessRouter;
import io.seata.saga.proctrl.ProcessType;
import io.seata.saga.proctrl.eventing.impl.AsyncEventBus;
import io.seata.saga.proctrl.eventing.impl.DirectEventBus;
import io.seata.saga.proctrl.eventing.impl.ProcessCtrlEventConsumer;
import io.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import io.seata.saga.proctrl.handler.DefaultRouterHandler;
import io.seata.saga.proctrl.handler.ProcessHandler;
import io.seata.saga.proctrl.handler.RouterHandler;
import io.seata.saga.proctrl.impl.ProcessControllerImpl;
import io.seata.saga.proctrl.process.impl.CustomizeBusinessProcessor;
import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.engine.repo.StateMachineRepository;
import io.seata.saga.engine.repo.impl.StateMachineRepositoryImpl;
import io.seata.saga.engine.sequence.SeqGenerator;
import io.seata.saga.engine.sequence.SpringJvmUUIDSeqGenerator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import io.seata.saga.statelang.domain.DomainConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * Default state machine configuration
 * @author lorne.cl
 */
public class DefaultStateMachineConfig implements StateMachineConfig, ApplicationContextAware, InitializingBean {

    private static final Logger       LOGGER = LoggerFactory.getLogger(DefaultStateMachineConfig.class);

    private StateLogStore             stateLogStore;
    private StateLangStore            stateLangStore;
    private ExpressionFactoryManager  expressionFactoryManager;
    private EvaluatorFactoryManager   evaluatorFactoryManager;
    private StateMachineRepository    stateMachineRepository;
    private StatusDecisionStrategy    statusDecisionStrategy;
    private SeqGenerator              seqGenerator;
    private ProcessCtrlEventPublisher syncProcessCtrlEventPublisher;
    private ProcessCtrlEventPublisher asyncProcessCtrlEventPublisher;
    private ApplicationContext        applicationContext;
    private ThreadPoolExecutor        threadPoolExecutor;
    private boolean                   enableAsync;
    private ServiceInvokerManager     serviceInvokerManager;

    private Resource[]                resources = new Resource[0];
    private String                    charset = "UTF-8";
    private String                    defaultTenantId = "000001";

    protected void init() throws Exception {

        if (seqGenerator == null) {
            seqGenerator = new SpringJvmUUIDSeqGenerator();
        }

        if (expressionFactoryManager == null) {
            expressionFactoryManager = new ExpressionFactoryManager();

            SpringELExpressionFactory springELExpressionFactory = new SpringELExpressionFactory();
            springELExpressionFactory.setApplicationContext(getApplicationContext());
            expressionFactoryManager.putExpressionFactory(ExpressionFactoryManager.DEFAULT_EXPRESSION_TYPE,
                springELExpressionFactory);

            SequenceExpressionFactory sequenceExpressionFactory = new SequenceExpressionFactory();
            sequenceExpressionFactory.setSeqGenerator(getSeqGenerator());
            expressionFactoryManager.putExpressionFactory(DomainConstants.EXPRESSION_TYPE_SEQUENCE, sequenceExpressionFactory);
        }

        if (evaluatorFactoryManager == null) {
            evaluatorFactoryManager = new EvaluatorFactoryManager();

            ExpressionEvaluatorFactory expressionEvaluatorFactory = new ExpressionEvaluatorFactory();
            expressionEvaluatorFactory.setExpressionFactory(
                expressionFactoryManager.getExpressionFactory(ExpressionFactoryManager.DEFAULT_EXPRESSION_TYPE));
            evaluatorFactoryManager.putEvaluatorFactory(EvaluatorFactoryManager.EVALUATOR_TYPE_DEFAULT, expressionEvaluatorFactory);

            evaluatorFactoryManager.putEvaluatorFactory(DomainConstants.EVALUATOR_TYPE_EXCEPTION, new ExceptionMatchEvaluatorFactory());
        }

        if (stateMachineRepository == null) {
            StateMachineRepositoryImpl stateMachineRepository = new StateMachineRepositoryImpl();
            stateMachineRepository.setCharset(charset);
            stateMachineRepository.setSeqGenerator(seqGenerator);
            stateMachineRepository.setStateLangStore(stateLangStore);
            stateMachineRepository.setDefaultTenantId(defaultTenantId);
            if (resources != null) {
                try {
                    stateMachineRepository.registryByResources(resources, defaultTenantId);
                } catch (IOException e) {
                    LOGGER.error("Load State Language Resources failed.", e);
                }
            }
            this.stateMachineRepository = stateMachineRepository;
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

        if(this.serviceInvokerManager == null){
            this.serviceInvokerManager = new ServiceInvokerManager();
        }
    }

    private ProcessControllerImpl createProcessorController(ProcessCtrlEventPublisher eventPublisher) throws Exception {

        StateMachineProcessRouter stateMachineProcessRouter = new StateMachineProcessRouter();
        stateMachineProcessRouter.initDefaultStateRouters();

        StateMachineProcessHandler stateMachineProcessHandler = new StateMachineProcessHandler();
        stateMachineProcessHandler.initDefaultHandlers();

        DefaultRouterHandler defaultRouterHandler = new DefaultRouterHandler();
        defaultRouterHandler.setEventPublisher(eventPublisher);

        Map<String, ProcessRouter> processRouterMap = new HashMap<>(1);
        processRouterMap.put(ProcessType.STATE_LANG.getCode(), stateMachineProcessRouter);
        defaultRouterHandler.setProcessRouters(processRouterMap);

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

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    @Override
    public StateLogStore getStateLogStore() {
        return this.stateLogStore;
    }

    @Override
    public StateLangStore getStateLangStore() {
        return stateLangStore;
    }

    @Override
    public ExpressionFactoryManager getExpressionFactoryManager() {
        return this.expressionFactoryManager;
    }

    @Override
    public EvaluatorFactoryManager getEvaluatorFactoryManager() {
        return this.evaluatorFactoryManager;
    }

    @Override
    public String getCharset() {
        return this.charset;
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
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    @Override
    public boolean isEnableAsync() {
        return enableAsync;
    }

    public void setStateLogStore(StateLogStore stateLogStore) {
        this.stateLogStore = stateLogStore;
    }

    public void setStateLangStore(StateLangStore stateLangStore) {
        this.stateLangStore = stateLangStore;
    }

    public void setExpressionFactoryManager(ExpressionFactoryManager expressionFactoryManager) {
        this.expressionFactoryManager = expressionFactoryManager;
    }

    public void setEvaluatorFactoryManager(EvaluatorFactoryManager evaluatorFactoryManager) {
        this.evaluatorFactoryManager = evaluatorFactoryManager;
    }

    public void setStateMachineRepository(StateMachineRepository stateMachineRepository) {
        this.stateMachineRepository = stateMachineRepository;
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

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public void setEnableAsync(boolean enableAsync) {
        this.enableAsync = enableAsync;
    }

    public void setResources(Resource[] resources) {
        this.resources = resources;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public ServiceInvokerManager getServiceInvokerManager() {
        return serviceInvokerManager;
    }

    public void setServiceInvokerManager(ServiceInvokerManager serviceInvokerManager) {
        this.serviceInvokerManager = serviceInvokerManager;
    }

    @Override
    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }
}