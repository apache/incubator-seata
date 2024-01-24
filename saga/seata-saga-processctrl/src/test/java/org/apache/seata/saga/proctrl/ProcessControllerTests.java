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
package org.apache.seata.saga.proctrl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.seata.saga.proctrl.eventing.impl.AsyncEventBus;
import org.apache.seata.saga.proctrl.eventing.impl.DirectEventBus;
import org.apache.seata.saga.proctrl.eventing.impl.ProcessCtrlEventConsumer;
import org.apache.seata.saga.proctrl.eventing.impl.ProcessCtrlEventPublisher;
import org.apache.seata.saga.proctrl.handler.DefaultRouterHandler;
import org.apache.seata.saga.proctrl.handler.ProcessHandler;
import org.apache.seata.saga.proctrl.handler.RouterHandler;
import org.apache.seata.saga.proctrl.impl.ProcessContextImpl;
import org.apache.seata.saga.proctrl.impl.ProcessControllerImpl;
import org.apache.seata.saga.proctrl.mock.MockInstruction;
import org.apache.seata.saga.proctrl.mock.MockProcessHandler;
import org.apache.seata.saga.proctrl.mock.MockProcessRouter;
import org.apache.seata.saga.proctrl.process.impl.CustomizeBusinessProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ProcessController Tests
 *
 */
public class ProcessControllerTests {

    @Test
    public void testSimpleProcessCtrl() {

        try {
            ProcessCtrlEventPublisher processCtrlEventPublisher = buildEventPublisher();

            ProcessContext context = new ProcessContextImpl();
            MockInstruction instruction = new MockInstruction();
            instruction.setTestString("one");
            context.setInstruction(instruction);
            context.setVariable("TEST", "test");

            processCtrlEventPublisher.publish(context);
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testSimpleProcessCtrlAsync() {

        try {
            ProcessCtrlEventPublisher processCtrlEventPublisher = buildAsyncEventPublisher();

            ProcessContext context = new ProcessContextImpl();
            MockInstruction instruction = new MockInstruction();
            instruction.setTestString("one");
            context.setInstruction(instruction);
            context.setVariable("TEST", "test");

            processCtrlEventPublisher.publish(context);
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    private ProcessCtrlEventPublisher buildEventPublisher() throws Exception {
        ProcessCtrlEventPublisher syncEventPublisher = new ProcessCtrlEventPublisher();

        ProcessControllerImpl processorController = createProcessorController(syncEventPublisher);

        ProcessCtrlEventConsumer processCtrlEventConsumer = new ProcessCtrlEventConsumer();
        processCtrlEventConsumer.setProcessController(processorController);

        DirectEventBus directEventBus = new DirectEventBus();
        syncEventPublisher.setEventBus(directEventBus);

        directEventBus.registerEventConsumer(processCtrlEventConsumer);

        return syncEventPublisher;
    }

    private ProcessCtrlEventPublisher buildAsyncEventPublisher() throws Exception {
        ProcessCtrlEventPublisher asyncEventPublisher = new ProcessCtrlEventPublisher();

        ProcessControllerImpl processorController = createProcessorController(asyncEventPublisher);

        ProcessCtrlEventConsumer processCtrlEventConsumer = new ProcessCtrlEventConsumer();
        processCtrlEventConsumer.setProcessController(processorController);

        AsyncEventBus asyncEventBus = new AsyncEventBus();
        asyncEventBus.setThreadPoolExecutor(
            new ThreadPoolExecutor(1, 5, 5000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()));

        asyncEventPublisher.setEventBus(asyncEventBus);

        asyncEventBus.registerEventConsumer(processCtrlEventConsumer);

        return asyncEventPublisher;
    }

    private ProcessControllerImpl createProcessorController(ProcessCtrlEventPublisher eventPublisher) throws Exception {

        DefaultRouterHandler defaultRouterHandler = new DefaultRouterHandler();
        defaultRouterHandler.setEventPublisher(eventPublisher);

        Map<String, ProcessRouter> processRouterMap = new HashMap<>(1);
        processRouterMap.put(ProcessType.STATE_LANG.getCode(), new MockProcessRouter());
        defaultRouterHandler.setProcessRouters(processRouterMap);

        CustomizeBusinessProcessor customizeBusinessProcessor = new CustomizeBusinessProcessor();

        Map<String, ProcessHandler> processHandlerMap = new HashMap<>(1);
        processHandlerMap.put(ProcessType.STATE_LANG.getCode(), new MockProcessHandler());
        customizeBusinessProcessor.setProcessHandlers(processHandlerMap);

        Map<String, RouterHandler> routerHandlerMap = new HashMap<>(1);
        routerHandlerMap.put(ProcessType.STATE_LANG.getCode(), defaultRouterHandler);
        customizeBusinessProcessor.setRouterHandlers(routerHandlerMap);

        ProcessControllerImpl processorController = new ProcessControllerImpl();
        processorController.setBusinessProcessor(customizeBusinessProcessor);

        return processorController;
    }
}
