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

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import io.seata.common.exception.FrameworkException;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.eventing.EventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronized EventBus
 *
 * @author lorne.cl
 */
public class AsyncEventBus extends AbstractEventBus<ProcessContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncEventBus.class);

    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public boolean offer(ProcessContext context) throws FrameworkException {

        List<EventConsumer> eventConsumers = getEventConsumers(context.getClass());
        if (eventConsumers == null || eventConsumers.size() == 0) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("cannot find event handler by class: " + context.getClass());
            }
            return false;
        }

        for (EventConsumer eventConsumer : eventConsumers) {

            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    eventConsumer.process(context);
                }
            });
        }
        return true;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }
}