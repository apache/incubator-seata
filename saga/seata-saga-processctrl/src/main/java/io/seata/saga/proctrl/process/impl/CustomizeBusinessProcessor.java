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
package io.seata.saga.proctrl.process.impl;

import java.util.Map;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.ProcessType;
import io.seata.saga.proctrl.handler.ProcessHandler;
import io.seata.saga.proctrl.handler.RouterHandler;
import io.seata.saga.proctrl.impl.ProcessControllerImpl;
import io.seata.saga.proctrl.process.BusinessProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizable Business Processor
 *
 * @author jin.xie
 * @author lorne.cl
 */
public class CustomizeBusinessProcessor implements BusinessProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessControllerImpl.class);

    private Map<String, ProcessHandler> processHandlers;

    private Map<String, RouterHandler> routerHandlers;

    public static ProcessType matchProcessType(ProcessContext context) {
        ProcessType processType = (ProcessType)context.getVariable(ProcessContext.VAR_NAME_PROCESS_TYPE);
        if (processType == null) {
            processType = ProcessType.STATE_LANG;
        }
        return processType;
    }

    @Override
    public void process(ProcessContext context) throws FrameworkException {

        ProcessType processType = matchProcessType(context);
        if (processType == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Process type not found, context= {}", context);
            }
            throw new FrameworkException(FrameworkErrorCode.ProcessTypeNotFound);
        }

        ProcessHandler processor = processHandlers.get(processType.getCode());
        if (processor == null) {
            LOGGER.error("Cannot find process handler by type {}, context= {}", processType.getCode(), context);
            throw new FrameworkException(FrameworkErrorCode.ProcessHandlerNotFound);
        }

        processor.process(context);
    }

    @Override
    public void route(ProcessContext context) throws FrameworkException {

        ProcessType processType = matchProcessType(context);
        if (processType == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Process type not found, the process is no longer advanced, context= {}", context);
            }
            return;
        }

        RouterHandler router = routerHandlers.get(processType.getCode());
        if (router == null) {
            LOGGER.error("Cannot find router handler by type {}, context= {}", processType.getCode(), context);
            return;
        }

        router.route(context);
    }

    public void setProcessHandlers(Map<String, ProcessHandler> processHandlers) {
        this.processHandlers = processHandlers;
    }

    public void setRouterHandlers(Map<String, RouterHandler> routerHandlers) {
        this.routerHandlers = routerHandlers;
    }
}
