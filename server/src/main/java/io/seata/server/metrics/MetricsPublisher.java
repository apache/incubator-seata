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

package io.seata.server.metrics;

import io.seata.core.event.EventBus;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.model.GlobalStatus;
import io.seata.server.event.EventBusManager;
import io.seata.server.session.GlobalSession;

/**
 * @author slievrly
 */
public class MetricsPublisher {

    private static final EventBus EVENT_BUS = EventBusManager.get();

    /**
     * post end event
     *
     * @param globalSession the global session
     */
    public static void postSessionDoneEvent(final GlobalSession globalSession, boolean retry) {
        postSessionDoneEvent(globalSession, globalSession.getStatus(), retry);
    }

    /**
     * post end event (force specified state)
     *
     * @param globalSession the global session
     * @param status        the global status
     */
    public static void postSessionDoneEvent(final GlobalSession globalSession, GlobalStatus status, boolean retry) {
        postSessionDoneEvent(globalSession, status.name(), retry, globalSession.getBeginTime());
    }

    /**
     * Post session done event.
     *
     * @param globalSession the global session
     * @param status        the status
     * @param retry         the retry
     * @param beginTime     the begin time
     */
    public static void postSessionDoneEvent(final GlobalSession globalSession, String status, boolean retry,
                                            long beginTime) {
        EVENT_BUS.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
            globalSession.getTransactionName(), globalSession.getApplicationId(),
            globalSession.getTransactionServiceGroup(), beginTime, System.currentTimeMillis(),
            status, retry));
    }

    /**
     * Post session doing event.
     *
     * @param globalSession the global session
     * @param retry         the retry
     */
    public static void postSessionDoingEvent(final GlobalSession globalSession, boolean retry) {
        postSessionDoingEvent(globalSession, globalSession.getStatus().name(), retry);
    }

    /**
     * Post session doing event.
     *
     * @param globalSession the global session
     * @param status        the status
     * @param retry         the retry
     */
    public static void postSessionDoingEvent(final GlobalSession globalSession, String status, boolean retry) {
        EVENT_BUS.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC, globalSession.getTransactionName(), globalSession.getApplicationId(),
            globalSession.getTransactionServiceGroup(), globalSession.getBeginTime(), null, status,retry));
    }
}
