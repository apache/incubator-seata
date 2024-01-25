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
package org.apache.seata.server.metrics;

import org.apache.seata.core.event.EventBus;
import org.apache.seata.core.event.GlobalTransactionEvent;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.event.EventBusManager;
import org.apache.seata.server.session.GlobalSession;

/**
 * The type Metrics publisher.
 *
 */
public class MetricsPublisher {

    private static final EventBus EVENT_BUS = EventBusManager.get();

    /**
     * post end event
     *
     * @param globalSession the global session
     * @param retryGlobal   the retry global
     * @param retryBranch   the retry branch
     */
    public static void postSessionDoneEvent(final GlobalSession globalSession, boolean retryGlobal,
                                            boolean retryBranch) {
        postSessionDoneEvent(globalSession, globalSession.getStatus(), retryGlobal, retryBranch);
    }

    /**
     * post end event (force specified state)
     *
     * @param globalSession the global session
     * @param status        the global status
     * @param retryGlobal   the retry global
     * @param retryBranch   the retry branch
     */
    public static void postSessionDoneEvent(final GlobalSession globalSession, GlobalStatus status, boolean retryGlobal,
                                            boolean retryBranch) {
        postSessionDoneEvent(globalSession, status.name(), retryGlobal, globalSession.getBeginTime(), retryBranch);
    }

    /**
     * Post session done event.
     *
     * @param globalSession the global session
     * @param status        the status
     * @param retryGlobal   the retry global
     * @param beginTime     the begin time
     * @param retryBranch   the retry branch
     */
    public static void postSessionDoneEvent(final GlobalSession globalSession, String status, boolean retryGlobal, long beginTime, boolean retryBranch) {
        EVENT_BUS.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
            globalSession.getTransactionName(), globalSession.getApplicationId(),
            globalSession.getTransactionServiceGroup(), beginTime, System.currentTimeMillis(), status, retryGlobal, retryBranch));
    }

    /**
     * Post session doing event.
     *
     * @param globalSession the global session
     * @param retryGlobal   the retry global
     */
    public static void postSessionDoingEvent(final GlobalSession globalSession, boolean retryGlobal) {
        postSessionDoingEvent(globalSession, globalSession.getStatus().name(), retryGlobal, false);
    }

    /**
     * Post session doing event.
     *
     * @param globalSession the global session
     * @param status        the status
     * @param retryGlobal   the retry global
     * @param retryBranch   the retry branch
     */
    public static void postSessionDoingEvent(final GlobalSession globalSession, String status, boolean retryGlobal,
                                             boolean retryBranch) {
        EVENT_BUS.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
            globalSession.getTransactionName(), globalSession.getApplicationId(),
            globalSession.getTransactionServiceGroup(), globalSession.getBeginTime(), null, status, retryGlobal, retryBranch));
    }
}
