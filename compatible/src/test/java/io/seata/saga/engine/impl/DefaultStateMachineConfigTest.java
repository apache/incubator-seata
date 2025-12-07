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

import io.seata.saga.engine.expression.ExpressionFactory;
import io.seata.saga.engine.expression.ExpressionFactoryManager;
import io.seata.saga.engine.repo.StateLogRepository;
import io.seata.saga.engine.repo.StateMachineRepository;
import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.engine.store.impl.StateLogStoreImpl;
import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionResolver;
import org.apache.seata.saga.engine.sequence.SeqGenerator;
import org.apache.seata.saga.engine.store.StateLangStore;
import org.apache.seata.saga.engine.strategy.StatusDecisionStrategy;
import org.apache.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.seata.saga.engine.config.AbstractStateMachineConfig.DEFAULT_SERVICE_INVOKE_TIMEOUT;
import static org.apache.seata.saga.engine.config.AbstractStateMachineConfig.DEFAULT_TRANS_OPERATION_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultStateMachineConfigTest {
    private DefaultStateMachineConfig defaultStateMachineConfig;

    @BeforeEach
    public void setUp() {
        defaultStateMachineConfig = new DefaultStateMachineConfig();
    }

    @Test
    public void testGetExpressionFactoryManager() {
        defaultStateMachineConfig.getExpressionFactoryManager();
        assertNotNull(defaultStateMachineConfig.getExpressionFactoryManager());

        ExpressionFactoryManager expressionFactoryManager = new ExpressionFactoryManager();
        ExpressionFactory factory = new ExpressionFactory() {
            @Override
            public Expression createExpression(String expression) {
                return new io.seata.saga.engine.expression.Expression() {
                    @Override
                    public Object getValue(Object elContext) {
                        return expression;
                    }

                    @Override
                    public void setValue(Object value, Object elContext) {}

                    @Override
                    public String getExpressionString() {
                        return expression;
                    }
                };
            }
        };
        Map<String, ExpressionFactory> expressionFactoryMap = new HashMap<>();
        expressionFactoryMap.put("type", factory);
        defaultStateMachineConfig.setExpressionResolver(new ExpressionResolver() {
            @Override
            public Expression getExpression(String expressionStr) {
                return null;
            }

            @Override
            public org.apache.seata.saga.engine.expression.ExpressionFactoryManager getExpressionFactoryManager() {
                return null;
            }

            @Override
            public void setExpressionFactoryManager(
                    org.apache.seata.saga.engine.expression.ExpressionFactoryManager expressionFactoryManager) {}
        });
        Assertions.assertNotNull(defaultStateMachineConfig.getExpressionResolver());
        expressionFactoryManager.setExpressionFactoryMap(expressionFactoryMap);
        defaultStateMachineConfig.setExpressionFactoryManager(expressionFactoryManager);
        ExpressionFactory retrievedFactory =
                defaultStateMachineConfig.getExpressionFactoryManager().getExpressionFactory("type");
        String mockValue = "mock";
        Assertions.assertEquals(
                mockValue, retrievedFactory.createExpression(mockValue).getExpressionString());
    }

    @Test
    void testGetStateMachineRepository() {
        defaultStateMachineConfig.setStateMachineRepository(null);
        StateMachineRepository repository = defaultStateMachineConfig.getStateMachineRepository();
        Assertions.assertNotNull(repository);

        String testStateMachineName = "testStateMachine";
        org.apache.seata.saga.statelang.domain.impl.StateMachineImpl actualStateMachine =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineImpl();
        actualStateMachine.setName(testStateMachineName);

        org.apache.seata.saga.engine.repo.StateMachineRepository stateMachineRepository =
                new org.apache.seata.saga.engine.repo.StateMachineRepository() {
                    @Override
                    public org.apache.seata.saga.statelang.domain.StateMachine getStateMachineById(String id) {
                        return actualStateMachine;
                    }

                    @Override
                    public org.apache.seata.saga.statelang.domain.StateMachine getStateMachine(
                            String name, String tenantId) {
                        return actualStateMachine;
                    }

                    @Override
                    public org.apache.seata.saga.statelang.domain.StateMachine getStateMachine(
                            String name, String tenantId, String version) {
                        return actualStateMachine;
                    }

                    @Override
                    public org.apache.seata.saga.statelang.domain.StateMachine registryStateMachine(
                            org.apache.seata.saga.statelang.domain.StateMachine stateMachine) {
                        return stateMachine;
                    }

                    @Override
                    public void registryByResources(java.io.InputStream[] resources, String tenantId) {}
                };

        defaultStateMachineConfig.setStateMachineRepository(stateMachineRepository);
        StateMachineRepository getRepository = defaultStateMachineConfig.getStateMachineRepository();
        Assertions.assertNotNull(getRepository);
        Assertions.assertEquals(
                testStateMachineName,
                getRepository.getStateMachineById(testStateMachineName).getName());
        Assertions.assertEquals(
                testStateMachineName,
                getRepository.getStateMachine(testStateMachineName, "").getName());
        Assertions.assertEquals(
                testStateMachineName,
                getRepository.getStateMachine(testStateMachineName, "", "").getName());
    }

    @Test
    public void testStateLogRepository() {
        defaultStateMachineConfig.setStateLogRepository(null);
        assertNotNull(defaultStateMachineConfig.getStateLogRepository());

        String testStateInstanceName = "testStateInstance";
        String testMachineId = "testMachineId";
        String testStateMachineInstanceName = "testStateMachineInstanceId";

        org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl actualStateInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateInstanceImpl();
        actualStateInstance.setName(testStateInstanceName);

        org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl actualStateMachineInstance =
                new org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl();
        actualStateMachineInstance.setMachineId(testMachineId);
        actualStateMachineInstance.setBusinessKey("key");

        org.apache.seata.saga.engine.repo.StateLogRepository stateLogRepository =
                new org.apache.seata.saga.engine.repo.StateLogRepository() {
                    @Override
                    public org.apache.seata.saga.statelang.domain.StateMachineInstance getStateMachineInstance(
                            String id) {
                        return actualStateMachineInstance;
                    }

                    @Override
                    public org.apache.seata.saga.statelang.domain.StateMachineInstance
                            getStateMachineInstanceByBusinessKey(String businessKey, String tenantId) {
                        return actualStateMachineInstance;
                    }

                    @Override
                    public java.util.List<org.apache.seata.saga.statelang.domain.StateMachineInstance>
                            queryStateMachineInstanceByParentId(String parentId) {
                        return java.util.Collections.emptyList();
                    }

                    @Override
                    public org.apache.seata.saga.statelang.domain.StateInstance getStateInstance(
                            String stateInstanceId, String machineInstId) {
                        return actualStateInstance;
                    }

                    @Override
                    public java.util.List<org.apache.seata.saga.statelang.domain.StateInstance>
                            queryStateInstanceListByMachineInstanceId(String stateMachineInstanceId) {
                        return java.util.Collections.emptyList();
                    }
                };

        defaultStateMachineConfig.setStateLogRepository(stateLogRepository);
        StateLogRepository getStateLogRepository = defaultStateMachineConfig.getStateLogRepository();
        Assertions.assertNotNull(getStateLogRepository);
        Assertions.assertEquals(
                testStateInstanceName,
                getStateLogRepository
                        .getStateInstance(testStateMachineInstanceName, "")
                        .getName());
        Assertions.assertEquals(
                testMachineId,
                getStateLogRepository
                        .getStateMachineInstance(testStateMachineInstanceName)
                        .getMachineId());
        Assertions.assertEquals(
                "key",
                getStateLogRepository
                        .getStateMachineInstanceByBusinessKey("key", "")
                        .getBusinessKey());
    }

    @Test
    public void testCharset() {
        String charset = defaultStateMachineConfig.getCharset();
        Assertions.assertEquals("UTF-8", charset);

        String newCharset = "ISO-8859-1";
        defaultStateMachineConfig.setCharset(newCharset);
        Assertions.assertEquals(newCharset, defaultStateMachineConfig.getCharset());
    }

    @Test
    public void testAsyncProcessCtrlEventPublisher() {
        ProcessCtrlEventPublisher asyncProcessCtrlEventPublisher =
                defaultStateMachineConfig.getAsyncProcessCtrlEventPublisher();
        Assertions.assertNull(asyncProcessCtrlEventPublisher);

        defaultStateMachineConfig.setAsyncProcessCtrlEventPublisher(new ProcessCtrlEventPublisher());
        Assertions.assertNotNull(defaultStateMachineConfig.getAsyncProcessCtrlEventPublisher());
    }

    @Test
    public void testGetExpressionResolver() {
        Assertions.assertNull(defaultStateMachineConfig.getExpressionResolver());

        ExpressionResolver expressionResolver = new ExpressionResolver() {
            @Override
            public org.apache.seata.saga.engine.expression.Expression getExpression(String expressionStr) {
                return null;
            }

            @Override
            public org.apache.seata.saga.engine.expression.ExpressionFactoryManager getExpressionFactoryManager() {
                return null;
            }

            @Override
            public void setExpressionFactoryManager(
                    org.apache.seata.saga.engine.expression.ExpressionFactoryManager expressionFactoryManager) {}
        };
        defaultStateMachineConfig.setExpressionResolver(expressionResolver);
        Assertions.assertEquals(expressionResolver, defaultStateMachineConfig.getExpressionResolver());
    }

    @Test
    public void testStatusDecisionStrategy() {
        Assertions.assertNull(defaultStateMachineConfig.getStatusDecisionStrategy());

        StatusDecisionStrategy statusDecisionStrategy =
                new org.apache.seata.saga.engine.strategy.impl.DefaultStatusDecisionStrategy();
        defaultStateMachineConfig.setStatusDecisionStrategy(statusDecisionStrategy);
        Assertions.assertEquals(statusDecisionStrategy, defaultStateMachineConfig.getStatusDecisionStrategy());
    }

    @Test
    public void testServiceInvokerManager() {
        Assertions.assertNull(defaultStateMachineConfig.getServiceInvokerManager());

        org.apache.seata.saga.engine.invoker.ServiceInvokerManager serviceInvokerManager =
                new org.apache.seata.saga.engine.invoker.ServiceInvokerManager();
        defaultStateMachineConfig.setServiceInvokerManager(serviceInvokerManager);
        Assertions.assertEquals(serviceInvokerManager, defaultStateMachineConfig.getServiceInvokerManager());
    }

    @Test
    public void testTransOperationTimeout() {
        Assertions.assertEquals(DEFAULT_TRANS_OPERATION_TIMEOUT, defaultStateMachineConfig.getTransOperationTimeout());

        int timeout = 1000;
        defaultStateMachineConfig.setTransOperationTimeout(timeout);
        Assertions.assertEquals(timeout, defaultStateMachineConfig.getTransOperationTimeout());
    }

    @Test
    public void testServiceInvokeTimeout() {
        Assertions.assertEquals(DEFAULT_SERVICE_INVOKE_TIMEOUT, defaultStateMachineConfig.getServiceInvokeTimeout());

        int timeout = 2000;
        defaultStateMachineConfig.setServiceInvokeTimeout(timeout);
        Assertions.assertEquals(timeout, defaultStateMachineConfig.getServiceInvokeTimeout());
    }

    @Test
    public void testScriptEngineManager() {
        Assertions.assertNull(defaultStateMachineConfig.getScriptEngineManager());

        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        defaultStateMachineConfig.setScriptEngineManager(scriptEngineManager);
        Assertions.assertEquals(scriptEngineManager, defaultStateMachineConfig.getScriptEngineManager());
    }

    @Test
    public void testAfterPropertiesSet() throws Exception {
        // Test that afterPropertiesSet doesn't throw exception
        defaultStateMachineConfig.afterPropertiesSet();
        assertNotNull(defaultStateMachineConfig.unwrap());
    }

    @Test
    public void testWrapAndUnwrap() {
        org.apache.seata.saga.engine.impl.DefaultStateMachineConfig actualConfig =
                new org.apache.seata.saga.engine.impl.DefaultStateMachineConfig();

        DefaultStateMachineConfig wrappedConfig = DefaultStateMachineConfig.wrap(actualConfig);
        assertNotNull(wrappedConfig);

        org.apache.seata.saga.engine.impl.DefaultStateMachineConfig unwrappedConfig = wrappedConfig.unwrap();
        assertEquals(actualConfig, unwrappedConfig);
    }

    @Test
    public void testGetStateLogStoreWithNull() {
        defaultStateMachineConfig.setStateLogStore(null);
        assertNull(defaultStateMachineConfig.getStateLogStore());
    }

    @Test
    public void testSetStateLogStoreWithNull() {
        defaultStateMachineConfig.setStateLogStore(null);
        assertNull(defaultStateMachineConfig.getStateLogStore());
    }

    @Test
    public void testStateLogStore() {
        org.apache.seata.saga.engine.store.StateLogStore actualStateLogStore =
                new org.apache.seata.saga.engine.store.db.DbAndReportTcStateLogStore();
        StateLogStore wrappedStateLogStore = StateLogStoreImpl.wrap(actualStateLogStore);

        defaultStateMachineConfig.setStateLogStore(wrappedStateLogStore);
        StateLogStore retrievedStateLogStore = defaultStateMachineConfig.getStateLogStore();
        assertNotNull(retrievedStateLogStore);
    }

    @Test
    public void testStateLangStore() {
        assertNull(defaultStateMachineConfig.getStateLangStore());

        StateLangStore stateLangStore = new org.apache.seata.saga.engine.store.db.DbStateLangStore();
        defaultStateMachineConfig.setStateLangStore(stateLangStore);
        assertEquals(stateLangStore, defaultStateMachineConfig.getStateLangStore());
    }

    @Test
    public void testSeqGenerator() {
        // afterPropertiesSet() creates a default UUIDSeqGenerator
        assertNotNull(defaultStateMachineConfig.getSeqGenerator());

        SeqGenerator newSeqGenerator = new SeqGenerator() {
            @Override
            public String generate(String entity) {
                return "test-seq-" + entity;
            }

            @Override
            public String generate(String entity, java.util.List<Object> shardingParameters) {
                return "test-seq-" + entity;
            }

            @Override
            public String generate(String entity, String ruleName, java.util.List<Object> shardingParameters) {
                return "test-seq-" + entity + "-" + ruleName;
            }
        };
        defaultStateMachineConfig.setSeqGenerator(newSeqGenerator);
        assertEquals(newSeqGenerator, defaultStateMachineConfig.getSeqGenerator());
    }

    @Test
    public void testProcessCtrlEventPublisher() {
        assertNull(defaultStateMachineConfig.getProcessCtrlEventPublisher());
    }

    @Test
    public void testSyncProcessCtrlEventPublisher() {
        ProcessCtrlEventPublisher publisher = new ProcessCtrlEventPublisher();
        defaultStateMachineConfig.setSyncProcessCtrlEventPublisher(publisher);
        assertNotNull(defaultStateMachineConfig.unwrap());
    }

    @Test
    public void testAutoRegisterResources() {
        defaultStateMachineConfig.setAutoRegisterResources(true);
        assertNotNull(defaultStateMachineConfig.unwrap());

        defaultStateMachineConfig.setAutoRegisterResources(false);
        assertNotNull(defaultStateMachineConfig.unwrap());
    }

    @Test
    public void testResources() {
        String[] resources = new String[] {"resource1", "resource2"};
        defaultStateMachineConfig.setResources(resources);
        assertNotNull(defaultStateMachineConfig.unwrap());
    }

    @Test
    public void testDefaultTenantId() {
        // afterPropertiesSet() sets default tenant ID to "000001"
        assertEquals("000001", defaultStateMachineConfig.getDefaultTenantId());

        String tenantId = "tenant123";
        defaultStateMachineConfig.setDefaultTenantId(tenantId);
        assertEquals(tenantId, defaultStateMachineConfig.getDefaultTenantId());
    }

    @Test
    public void testApplicationContext() {
        assertNull(defaultStateMachineConfig.getApplicationContext());

        ApplicationContext context = new org.springframework.context.support.StaticApplicationContext();
        defaultStateMachineConfig.setApplicationContext(context);
        assertEquals(context, defaultStateMachineConfig.getApplicationContext());
    }

    @Test
    public void testThreadPoolExecutor() {
        assertNull(defaultStateMachineConfig.getThreadPoolExecutor());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 60L, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        defaultStateMachineConfig.setThreadPoolExecutor(executor);
        assertEquals(executor, defaultStateMachineConfig.getThreadPoolExecutor());
        executor.shutdown();
    }

    @Test
    public void testEnableAsync() {
        assertFalse(defaultStateMachineConfig.isEnableAsync());

        defaultStateMachineConfig.setEnableAsync(true);
        assertTrue(defaultStateMachineConfig.isEnableAsync());

        defaultStateMachineConfig.setEnableAsync(false);
        assertFalse(defaultStateMachineConfig.isEnableAsync());
    }

    @Test
    public void testSagaJsonParser() {
        // afterPropertiesSet() sets default parser to "fastjson"
        assertEquals("fastjson", defaultStateMachineConfig.getSagaJsonParser());

        String parser = "jackson";
        defaultStateMachineConfig.setSagaJsonParser(parser);
        assertEquals(parser, defaultStateMachineConfig.getSagaJsonParser());
    }

    @Test
    public void testSagaRetryPersistModeUpdate() {
        assertFalse(defaultStateMachineConfig.isSagaRetryPersistModeUpdate());

        defaultStateMachineConfig.setSagaRetryPersistModeUpdate(true);
        assertTrue(defaultStateMachineConfig.isSagaRetryPersistModeUpdate());

        defaultStateMachineConfig.setSagaRetryPersistModeUpdate(false);
        assertFalse(defaultStateMachineConfig.isSagaRetryPersistModeUpdate());
    }

    @Test
    public void testSagaCompensatePersistModeUpdate() {
        assertFalse(defaultStateMachineConfig.isSagaCompensatePersistModeUpdate());

        defaultStateMachineConfig.setSagaCompensatePersistModeUpdate(true);
        assertTrue(defaultStateMachineConfig.isSagaCompensatePersistModeUpdate());

        defaultStateMachineConfig.setSagaCompensatePersistModeUpdate(false);
        assertFalse(defaultStateMachineConfig.isSagaCompensatePersistModeUpdate());
    }
}
