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
package io.seata.saga.proctrl.handler;

import java.util.Map;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.saga.proctrl.Instruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.ProcessRouter;
import io.seata.saga.proctrl.ProcessType;
import io.seata.saga.proctrl.eventing.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Router handler
 *
 * @author jin.xie
 * @author lorne.cl
 */
public class DefaultRouterHandler implements RouterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRouterHandler.class);

    private EventPublisher<ProcessContext> eventPublisher;
    private Map<String, ProcessRouter> processRouters;

    public static ProcessType matchProcessType(ProcessContext context) {
        ProcessType processType = (ProcessType)context.getVariable(ProcessContext.VAR_NAME_PROCESS_TYPE);
        if (processType == null) {
            processType = ProcessType.STATE_LANG;
        }
        return processType;
    }

    @Override
    public void route(ProcessContext context) throws FrameworkException {

        try {
            ProcessType processType = matchProcessType(context);
            if (processType == null) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Process type not found, context= {}", context);
                }
                throw new FrameworkException(FrameworkErrorCode.ProcessTypeNotFound);
            }

            ProcessRouter processRouter = processRouters.get(processType.getCode());
            if (processRouter == null) {
                LOGGER.error("Cannot find process router by type {}, context = {}", processType.getCode(), context);
                throw new FrameworkException(FrameworkErrorCode.ProcessRouterNotFound);
            }

            Instruction instruction = processRouter.route(context);
            if (instruction == null) {
                LOGGER.warn("route instruction is null, process end");
            } else {
                context.setInstruction(instruction);

                eventPublisher.publish(context);
            }
        } catch (FrameworkException e) {
            throw e;
        } catch (Exception ex) {
            throw new FrameworkException(ex, ex.getMessage(), FrameworkErrorCode.UnknownAppError);
        }
    }

    public void setEventPublisher(EventPublisher<ProcessContext> eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void setProcessRouters(Map<String, ProcessRouter> processRouters) {
        this.processRouters = processRouters;
    }
}
