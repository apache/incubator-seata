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

import org.apache.seata.core.event.*;
import org.apache.seata.metrics.*;
import org.apache.seata.metrics.registry.Registry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetricsSubscriberUnitTest {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "CommitFailed",
                "RollbackFailed",
                "TimeoutRollbackFailed",
                "CommitRetryTimeout",
                "RollbackRetryTimeout"
            })
    void failuresDecreaseActiveCountAndRecordFailureLatency(String status) {
        Registry registry = mock(Registry.class);
        Counter counter = mock(Counter.class);
        Summary summary = mock(Summary.class);
        Timer timer = mock(Timer.class);
        when(registry.getCounter(any())).thenReturn(counter);
        when(registry.getSummary(any())).thenReturn(summary);
        when(registry.getTimer(any())).thenReturn(timer);
        MetricsSubscriber subscriber = new MetricsSubscriber(registry);
        subscriber.recordGlobalTransactionEventForMetrics(
                new GlobalTransactionEvent(1, "tc", "order", "app", "group", 10L, 40L, status, false, false));
        verify(counter).decrease(1);
        verify(timer).record(30, TimeUnit.MILLISECONDS);
        verify(summary, atLeastOnce()).increase(1);
        verify(registry)
                .getSummary(MeterIdConstants.SUMMARY_FAILED
                        .withTag(IdConstants.APP_ID_KEY, "app")
                        .withTag(IdConstants.GROUP_KEY, "group"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CommitRetrying", "RollbackRetrying", "TimeoutRollbackRetrying"})
    void retriesIncludeTransactionNameWithoutCompletingActiveTransaction(String status) {
        Registry registry = mock(Registry.class);
        Summary summary = mock(Summary.class);
        when(registry.getSummary(any())).thenReturn(summary);
        new MetricsSubscriber(registry)
                .recordGlobalTransactionEventForMetrics(
                        new GlobalTransactionEvent(1, "tc", "order", "app", "group", 10L, 40L, status, true, true));
        verify(summary).increase(1);
        verify(registry, never()).getCounter(any());
        verify(registry, never()).getTimer(any());
    }

    @Test
    void exceptionsRateLimitsAndUnknownStatusesAreHandled() {
        Registry registry = mock(Registry.class);
        Summary summary = mock(Summary.class);
        when(registry.getSummary(any())).thenReturn(summary);
        MetricsSubscriber subscriber = new MetricsSubscriber(registry);
        subscriber.exceptionEventForMetrics(new ExceptionEvent("failure"));
        subscriber.recordRateLimitEventForMetrics(new RateLimitEvent("trace", "token", "app", "host:8091"));
        verify(summary, times(2)).increase(1);
        clearInvocations(registry);
        subscriber.recordGlobalTransactionEventForMetrics(
                new GlobalTransactionEvent(1, "tc", "order", "app", "group", 10L, 40L, "unknown", false, false));
        verifyNoInteractions(registry);
        assertDoesNotThrow(() -> new MetricsSubscriber(null).recordGlobalTransactionEventForMetrics(null));
    }

    @Test
    void retryCompletionIsCountedOnlyAfterBothPhasesFinish() {
        Registry registry = mock(Registry.class);
        Counter counter = mock(Counter.class);
        Summary summary = mock(Summary.class);
        Timer timer = mock(Timer.class);
        when(registry.getCounter(any())).thenReturn(counter);
        when(registry.getSummary(any())).thenReturn(summary);
        when(registry.getTimer(any())).thenReturn(timer);
        MetricsSubscriber subscriber = new MetricsSubscriber(registry);
        for (String status : new String[] {"Committed", "Rollbacked"}) {
            subscriber.recordGlobalTransactionEventForMetrics(
                    new GlobalTransactionEvent(1, "tc", "order", "app", "group", 10L, 40L, status, true, true));
        }
        verifyNoInteractions(counter, summary, timer);
        for (String status : new String[] {
            IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY
        }) {
            subscriber.recordGlobalTransactionEventForMetrics(
                    new GlobalTransactionEvent(1, "tc", "order", "app", "group", 10L, 40L, status, true, true));
        }
        verify(counter, times(2)).decrease(1);
        verify(counter, times(2)).increase(1);
        verify(summary, times(2)).increase(1);
        verify(timer, times(2)).record(30, TimeUnit.MILLISECONDS);
        clearInvocations(counter, summary, timer);
        subscriber.recordGlobalTransactionEventForMetrics(new GlobalTransactionEvent(
                1, "tc", "order", "app", "group", 10L, 40L, "TimeoutRollbacked", false, false));
        verify(counter).decrease(1);
        verifyNoInteractions(summary, timer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Committed", "Rollbacked"})
    void firstCompletionRecordsSuccessAndLatency(String status) {
        Registry registry = mock(Registry.class);
        Counter counter = mock(Counter.class);
        Summary summary = mock(Summary.class);
        Timer timer = mock(Timer.class);
        when(registry.getCounter(any())).thenReturn(counter);
        when(registry.getSummary(any())).thenReturn(summary);
        when(registry.getTimer(any())).thenReturn(timer);
        new MetricsSubscriber(registry)
                .recordGlobalTransactionEventForMetrics(
                        new GlobalTransactionEvent(1, "tc", "order", "app", "group", 10L, 40L, status, false, false));
        verify(counter).decrease(1);
        verify(counter).increase(1);
        verify(summary).increase(1);
        verify(timer).record(30, TimeUnit.MILLISECONDS);
    }
}
