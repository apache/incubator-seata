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

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngineManager;

import io.seata.saga.engine.expression.ExpressionFactory;
import io.seata.saga.engine.expression.ExpressionFactoryManager;
import io.seata.saga.engine.repo.StateLogRepository;
import io.seata.saga.engine.repo.StateMachineRepository;
import org.apache.seata.saga.engine.expression.Expression;
import org.apache.seata.saga.engine.expression.ExpressionResolver;
import org.apache.seata.saga.engine.strategy.StatusDecisionStrategy;
import org.apache.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.seata.saga.engine.config.AbstractStateMachineConfig.DEFAULT_SERVICE_INVOKE_TIMEOUT;
import static org.apache.seata.saga.engine.config.AbstractStateMachineConfig.DEFAULT_TRANS_OPERATION_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                    public void setValue(Object value, Object elContext) {

                    }

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
                org.apache.seata.saga.engine.expression.ExpressionFactoryManager expressionFactoryManager) {

            }
        });
        Assertions.assertNotNull(defaultStateMachineConfig.getExpressionResolver());
        expressionFactoryManager.setExpressionFactoryMap(expressionFactoryMap);
        defaultStateMachineConfig.setExpressionFactoryManager(expressionFactoryManager);
        ExpressionFactory retrievedFactory = defaultStateMachineConfig.getExpressionFactoryManager()
            .getExpressionFactory("type");
        String mockValue = "mock";
        Assertions.assertEquals(mockValue, retrievedFactory.createExpression(mockValue).getExpressionString());

    }

    @Test
    void testGetStateMachineRepository() {
        defaultStateMachineConfig.setStateMachineRepository(null);
        StateMachineRepository repository = defaultStateMachineConfig.getStateMachineRepository();
        Assertions.assertNotNull(repository);

        String mockStateMachineName = "mockStateMachine";
        StateMachine mockStateMachine = mock(StateMachine.class);
        when(mockStateMachine.getName()).thenReturn(mockStateMachineName);
        org.apache.seata.saga.engine.repo.StateMachineRepository sateMachineRepository = mock(
            org.apache.seata.saga.engine.repo.StateMachineRepository.class);
        when(sateMachineRepository.getStateMachine(mockStateMachineName, "", "")).thenReturn(mockStateMachine);
        when(sateMachineRepository.getStateMachine(mockStateMachineName, "")).thenReturn(mockStateMachine);
        when(sateMachineRepository.getStateMachineById(mockStateMachineName)).thenReturn(mockStateMachine);
        defaultStateMachineConfig.setStateMachineRepository(sateMachineRepository);
        StateMachineRepository getRepository = defaultStateMachineConfig.getStateMachineRepository();
        Assertions.assertNotNull(getRepository);
        Assertions.assertEquals(mockStateMachineName,
            getRepository.getStateMachineById(mockStateMachineName).getName());
        Assertions.assertEquals(mockStateMachineName,
            getRepository.getStateMachine(mockStateMachineName, "").getName());
        Assertions.assertEquals(mockStateMachineName,
            getRepository.getStateMachine(mockStateMachineName, "", "").getName());
    }

    @Test
    public void testStateLogRepository() {
        defaultStateMachineConfig.setStateLogRepository(null);
        assertNotNull(defaultStateMachineConfig.getStateLogRepository());

        org.apache.seata.saga.engine.repo.StateLogRepository mockStateLogRepository = mock(
            org.apache.seata.saga.engine.repo.StateLogRepository.class);
        defaultStateMachineConfig.setStateLogRepository(mockStateLogRepository);
        StateInstance mockStateInstance = mock(StateInstance.class);
        StateMachineInstance mockStateMachineInstance = mock(StateMachineInstance.class);
        String mockStateInstanceName = "mockStateInstance";
        String mockMachineId = "mockMachineId";
        when(mockStateMachineInstance.getMachineId()).thenReturn(mockMachineId);
        when(mockStateMachineInstance.getBusinessKey()).thenReturn("key");
        String mockStateMachineInstanceName = "mockStateMachineInstanceId";
        when(mockStateInstance.getName()).thenReturn(mockStateInstanceName);
        when(mockStateLogRepository.getStateInstance(mockStateMachineInstanceName, "")).thenReturn(mockStateInstance);
        when(mockStateLogRepository.getStateMachineInstance(mockStateMachineInstanceName)).thenReturn(
            mockStateMachineInstance);
        when(mockStateLogRepository.getStateMachineInstanceByBusinessKey("key", "")).thenReturn(
            mockStateMachineInstance);
        StateLogRepository getStateLogRepository = defaultStateMachineConfig.getStateLogRepository();
        Assertions.assertNotNull(getStateLogRepository);
        Assertions.assertEquals(mockStateInstanceName,
            getStateLogRepository.getStateInstance(mockStateMachineInstanceName, "").getName());
        Assertions.assertEquals(mockMachineId,
            getStateLogRepository.getStateMachineInstance(mockStateMachineInstanceName).getMachineId());
        Assertions.assertEquals("key",
            getStateLogRepository.getStateMachineInstanceByBusinessKey("key", "").getBusinessKey());
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
        ProcessCtrlEventPublisher asyncProcessCtrlEventPublisher
            = defaultStateMachineConfig.getAsyncProcessCtrlEventPublisher();
        Assertions.assertNull(asyncProcessCtrlEventPublisher);

        defaultStateMachineConfig.setAsyncProcessCtrlEventPublisher(new ProcessCtrlEventPublisher());
        Assertions.assertNotNull(defaultStateMachineConfig.getAsyncProcessCtrlEventPublisher());
    }

    @Test
    public void testGetExpressionResolver() {
        Assertions.assertNull(defaultStateMachineConfig.getExpressionResolver());

        ExpressionResolver expressionResolver = mock(ExpressionResolver.class);
        defaultStateMachineConfig.setExpressionResolver(expressionResolver);
        Assertions.assertEquals(expressionResolver, defaultStateMachineConfig.getExpressionResolver());
    }

    @Test
    public void testStatusDecisionStrategy() {
        Assertions.assertNull(defaultStateMachineConfig.getStatusDecisionStrategy());

        StatusDecisionStrategy statusDecisionStrategy = mock(StatusDecisionStrategy.class);
        defaultStateMachineConfig.setStatusDecisionStrategy(statusDecisionStrategy);
        Assertions.assertEquals(statusDecisionStrategy, defaultStateMachineConfig.getStatusDecisionStrategy());
    }

    @Test
    public void testServiceInvokerManager() {
        Assertions.assertNull(defaultStateMachineConfig.getServiceInvokerManager());

        org.apache.seata.saga.engine.invoker.ServiceInvokerManager serviceInvokerManager = mock(
            org.apache.seata.saga.engine.invoker.ServiceInvokerManager.class);
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

        ScriptEngineManager scriptEngineManager = mock(ScriptEngineManager.class);
        defaultStateMachineConfig.setScriptEngineManager(scriptEngineManager);
        Assertions.assertEquals(scriptEngineManager, defaultStateMachineConfig.getScriptEngineManager());
    }
}