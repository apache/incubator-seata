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
package org.apache.seata.server.coordinator.pipeline;

/**
 * Outcome of submitting a {@link org.apache.seata.server.session.GlobalSession} to
 * {@link PhaseTwoPipelineExecutor#tryOffer(org.apache.seata.server.session.GlobalSession)}.
 *
 * <p>Exposes explicit reasons so callers such as the Wave 2 fallback recovery scanner (#8357)
 * can apply backpressure immediately on {@link #QUEUE_FULL} while treating {@link #DUPLICATE}
 * as a normal deduplication skip.
 */
public enum OfferResult {
    /**
     * Session acquired a queued in-flight lease and was accepted into the bounded task queue.
     * Note: if {@link PhaseTwoPipelineExecutor#shutdown(long)} races concurrently with submission,
     * a queued session may be released back to fallback scanning rather than executed.
     */
    ACCEPTED,

    /**
     * Session XID already holds an active in-flight lease in {@link InFlightSessionRegistry}.
     */
    DUPLICATE,

    /**
     * Bounded task queue is currently at capacity; the acquired lease was immediately released
     * and the drop counter incremented.
     */
    QUEUE_FULL,

    /**
     * Executor has not been started yet or has already been shut down.
     */
    NOT_RUNNING,

    /**
     * Session was {@code null} or had a {@code null}/empty XID.
     */
    INVALID
}
