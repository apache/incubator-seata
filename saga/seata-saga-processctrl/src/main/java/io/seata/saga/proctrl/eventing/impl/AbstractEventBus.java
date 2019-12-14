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

import java.util.ArrayList;
import java.util.List;

import io.seata.saga.proctrl.eventing.EventBus;
import io.seata.saga.proctrl.eventing.EventConsumer;

/**
 * Abstract Event Bus
 *
 * @author jin.xie
 * @author lorne.cl
 */
public abstract class AbstractEventBus<E> implements EventBus<E> {

    private List<EventConsumer> eventConsumerList = new ArrayList<>();

    @Override
    public List<EventConsumer> getEventConsumers(Class clazz) {

        List<EventConsumer> acceptedConsumers = new ArrayList<>();
        for (EventConsumer eventConsumer : eventConsumerList) {
            if (eventConsumer.accept(clazz)) {
                acceptedConsumers.add(eventConsumer);
            }
        }
        return acceptedConsumers;
    }

    @Override
    public void registerEventConsumer(EventConsumer eventConsumer) {
        eventConsumerList.add(eventConsumer);
    }
}