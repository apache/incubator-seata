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

import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-RM ({@code resourceId}) concurrency limiter for phase-two RPC execution.
 *
 * <p>Limits concurrent phase-two RPCs targeting the same resource manager so a slow or unhealthy
 * RM cannot exhaust the shared {@code recovery-rpc} worker pool and starve healthy RMs.
 *
 * <p>For multi-RM global transactions, permits are always acquired in deterministic lexicographical
 * order of distinct {@code resourceId}s, guaranteeing deadlock freedom across concurrent workers.
 */
public class RmConcurrencyLimiter {

    private static final Permit NOOP_PERMIT = new Permit(Collections.emptyList());

    private final int maxConcurrentPerRm;
    private final ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();

    /**
     * Creates a per-RM concurrency limiter.
     *
     * @param maxConcurrentPerRm maximum concurrent workers per {@code resourceId};
     *                           values {@code <= 0} disable per-RM limiting
     */
    public RmConcurrencyLimiter(int maxConcurrentPerRm) {
        this.maxConcurrentPerRm = Math.max(0, maxConcurrentPerRm);
    }

    /**
     * Returns whether per-RM concurrency limiting is active.
     *
     * @return {@code true} when {@code maxConcurrentPerRm > 0}
     */
    public boolean isEnabled() {
        return maxConcurrentPerRm > 0;
    }

    public int getMaxConcurrentPerRm() {
        return maxConcurrentPerRm;
    }

    /**
     * Acquires one permit for each distinct {@code resourceId} present in {@code session}'s branch
     * sessions in sorted lexicographical order.
     *
     * @param session global session whose branches determine the target resource IDs
     * @return an {@link AutoCloseable} permit handle releasing all acquired permits on close
     * @throws InterruptedException if interrupted while waiting; any partially acquired permits
     *                              are released before rethrowing
     */
    public Permit acquire(GlobalSession session) throws InterruptedException {
        if (!isEnabled() || session == null) {
            return NOOP_PERMIT;
        }
        return acquire(extractSortedResourceIds(session));
    }

    /**
     * Acquires one permit for each distinct non-blank {@code resourceId} in {@code resourceIds}
     * in sorted lexicographical order.
     *
     * @param resourceIds collection of resource IDs
     * @return an {@link AutoCloseable} permit handle releasing all acquired permits on close
     * @throws InterruptedException if interrupted while waiting; any partially acquired permits
     *                              are released before rethrowing
     */
    public Permit acquire(Collection<String> resourceIds) throws InterruptedException {
        if (!isEnabled() || resourceIds == null || resourceIds.isEmpty()) {
            return NOOP_PERMIT;
        }
        List<String> sortedIds = normalizeAndSort(resourceIds);
        if (sortedIds.isEmpty()) {
            return NOOP_PERMIT;
        }
        List<Semaphore> acquired = new ArrayList<>(sortedIds.size());
        try {
            for (String resourceId : sortedIds) {
                Semaphore semaphore =
                        semaphores.computeIfAbsent(resourceId, key -> new Semaphore(maxConcurrentPerRm, true));
                semaphore.acquire();
                acquired.add(semaphore);
            }
            return new Permit(acquired);
        } catch (InterruptedException e) {
            releaseAll(acquired);
            throw e;
        } catch (Throwable t) {
            releaseAll(acquired);
            throw t;
        }
    }

    /**
     * Extracts distinct, non-blank {@code resourceId}s from the session's branches in sorted order.
     *
     * @param session global session
     * @return sorted list of distinct resource IDs
     */
    public static List<String> extractSortedResourceIds(GlobalSession session) {
        if (session == null) {
            return Collections.emptyList();
        }
        List<BranchSession> branches = session.getBranchSessions();
        if (branches == null || branches.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> sorted = new TreeSet<>();
        for (BranchSession branch : branches) {
            if (branch != null) {
                String resourceId = branch.getResourceId();
                if (resourceId != null && !resourceId.trim().isEmpty()) {
                    sorted.add(resourceId);
                }
            }
        }
        return sorted.isEmpty() ? Collections.emptyList() : new ArrayList<>(sorted);
    }

    private static List<String> normalizeAndSort(Collection<String> resourceIds) {
        Set<String> sorted = new TreeSet<>();
        for (String resourceId : resourceIds) {
            if (resourceId != null && !resourceId.trim().isEmpty()) {
                sorted.add(resourceId);
            }
        }
        return sorted.isEmpty() ? Collections.emptyList() : new ArrayList<>(sorted);
    }

    private static void releaseAll(List<Semaphore> acquired) {
        for (int i = acquired.size() - 1; i >= 0; i--) {
            acquired.get(i).release();
        }
    }

    /**
     * Returns the number of distinct resource IDs currently tracked by this limiter.
     *
     * @return tracked resource count
     */
    public int trackedResourceCount() {
        return semaphores.size();
    }

    /**
     * Idempotent permit handle releasing all acquired per-RM semaphores upon {@link #close()}.
     */
    public static final class Permit implements AutoCloseable {

        private final List<Semaphore> semaphores;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private Permit(List<Semaphore> semaphores) {
            this.semaphores = semaphores;
        }

        @Override
        public void close() {
            if (!semaphores.isEmpty() && released.compareAndSet(false, true)) {
                releaseAll(semaphores);
            }
        }
    }
}
