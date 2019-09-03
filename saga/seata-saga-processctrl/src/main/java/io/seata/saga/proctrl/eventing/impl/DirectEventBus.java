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
package io.seata.saga.proctrl.eventing.impl;

import io.seata.common.exception.FrameworkException;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.eventing.EventConsumer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deliver event to event consumer directly
 *
 * @author lorne.cl
 */
public class DirectEventBus extends AbstractEventBus<ProcessContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectEventBus.class);

    @Override
    public boolean offer(ProcessContext context) throws FrameworkException {
        List<EventConsumer> eventHandlers = getEventConsumers(context.getClass());
        if(eventHandlers == null || eventHandlers.size() == 0){
            if(LOGGER.isWarnEnabled()){
                LOGGER.warn("cannot find event handler by class: " + context.getClass());
            }
            return false;
        }

        for(EventConsumer eventHandler : eventHandlers){

            eventHandler.process(context);
        }
        return true;
    }
}