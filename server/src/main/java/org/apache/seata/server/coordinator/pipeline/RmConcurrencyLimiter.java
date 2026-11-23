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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-RM ({@code resourceId}) concurrency limiter for phase-two RPC execution.
 *
 * <p>Limits concurrent phase-two RPCs targeting the same resource manager so a slow or unhealthy
 * RM cannot exhaust the shared {@code recovery-rpc} worker pool and starve healthy RMs.
 *
 * <p>For multi-RM global transactions, permits are always acquired in deterministic lexicographical
 * order of distinct {@code resourceId}s, guaranteeing deadlock freedom across concurrent workers.
 * Prefer {@link #tryAcquire(GlobalSession)} on worker threads so a throttled RM never blocks
 * shared worker threads needed by healthy RMs.
 *
 * <p>Idle per-resource entries are automatically evicted once all active holders and waiters for
 * that {@code resourceId} have released, bounding memory usage even under dynamic/multi-tenant
 * resource IDs.
 */
public class RmConcurrencyLimiter {

    private static final Permit NOOP_PERMIT = new Permit(null, Collections.emptyList());

    private final int maxConcurrentPerRm;
    private final ConcurrentHashMap<String, ResourceSlot> slots = new ConcurrentHashMap<>();

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
     * Non-blockingly attempts to acquire one permit for each distinct {@code resourceId} present
     * in {@code session}'s branch sessions in sorted lexicographical order.
     *
     * <p>If any resource is currently at capacity, all previously acquired permits in this call
     * are immediately rolled back and {@link Optional#empty()} is returned.
     *
     * @param session global session whose branches determine the target resource IDs
     * @return an {@link Optional} containing a permit handle if all resources had capacity, or
     *         {@link Optional#empty()} if any target RM is throttled
     */
    public Optional<Permit> tryAcquire(GlobalSession session) {
        if (!isEnabled() || session == null) {
            return Optional.of(NOOP_PERMIT);
        }
        return tryAcquire(extractSortedResourceIds(session));
    }

    /**
     * Non-blockingly attempts to acquire one permit for each distinct non-blank {@code resourceId}
     * in {@code resourceIds} in sorted lexicographical order.
     *
     * <p>If any resource is currently at capacity, all previously acquired permits in this call
     * are immediately rolled back and {@link Optional#empty()} is returned.
     *
     * @param resourceIds collection of resource IDs
     * @return an {@link Optional} containing a permit handle if all resources had capacity, or
     *         {@link Optional#empty()} if any target RM is throttled
     */
    public Optional<Permit> tryAcquire(Collection<String> resourceIds) {
        if (!isEnabled() || resourceIds == null || resourceIds.isEmpty()) {
            return Optional.of(NOOP_PERMIT);
        }
        List<String> sortedIds = normalizeAndSort(resourceIds);
        if (sortedIds.isEmpty()) {
            return Optional.of(NOOP_PERMIT);
        }
        List<AcquiredSlot> acquired = new ArrayList<>(sortedIds.size());
        try {
            for (String resourceId : sortedIds) {
                ResourceSlot slot = retainSlot(resourceId);
                if (!slot.semaphore.tryAcquire()) {
                    releaseSlotReference(resourceId, slot);
                    releaseAll(acquired);
                    return Optional.empty();
                }
                acquired.add(new AcquiredSlot(resourceId, slot));
            }
            return Optional.of(new Permit(this, acquired));
        } catch (Throwable t) {
            releaseAll(acquired);
            throw t;
        }
    }

    /**
     * Blockingly acquires one permit for each distinct {@code resourceId} present in
     * {@code session}'s branch sessions in sorted lexicographical order.
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
     * Blockingly acquires one permit for each distinct non-blank {@code resourceId} in
     * {@code resourceIds} in sorted lexicographical order.
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
        List<AcquiredSlot> acquired = new ArrayList<>(sortedIds.size());
        try {
            for (String resourceId : sortedIds) {
                ResourceSlot slot = retainSlot(resourceId);
                try {
                    slot.semaphore.acquire();
                } catch (InterruptedException | Error | RuntimeException ex) {
                    releaseSlotReference(resourceId, slot);
                    throw ex;
                }
                acquired.add(new AcquiredSlot(resourceId, slot));
            }
            return new Permit(this, acquired);
        } catch (InterruptedException e) {
            releaseAll(acquired);
            throw e;
        } catch (Throwable t) {
            releaseAll(acquired);
            throw t;
        }
    }

    private ResourceSlot retainSlot(String resourceId) {
        return slots.compute(resourceId, (key, existing) -> {
            ResourceSlot slot = existing != null ? existing : new ResourceSlot(maxConcurrentPerRm);
            slot.inUse.incrementAndGet();
            return slot;
        });
    }

    private void releaseSlotReference(String resourceId, ResourceSlot target) {
        slots.computeIfPresent(resourceId, (key, existing) -> {
            if (existing == target && target.inUse.decrementAndGet() == 0) {
                return null;
            }
            return existing;
        });
    }

    private void releaseAll(List<AcquiredSlot> acquired) {
        for (int i = acquired.size() - 1; i >= 0; i--) {
            AcquiredSlot entry = acquired.get(i);
            entry.slot.semaphore.release();
            releaseSlotReference(entry.resourceId, entry.slot);
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

    /**
     * Returns the number of distinct resource IDs currently active or waited on in this limiter.
     *
     * @return active tracked resource count
     */
    public int trackedResourceCount() {
        return slots.size();
    }

    private static final class ResourceSlot {
        private final Semaphore semaphore;
        private final AtomicInteger inUse = new AtomicInteger(0);

        private ResourceSlot(int maxPermits) {
            this.semaphore = new Semaphore(maxPermits, true);
        }
    }

    private static final class AcquiredSlot {
        private final String resourceId;
        private final ResourceSlot slot;

        private AcquiredSlot(String resourceId, ResourceSlot slot) {
            this.resourceId = resourceId;
            this.slot = slot;
        }
    }

    /**
     * Idempotent permit handle releasing all acquired per-RM semaphores upon {@link #close()}.
     */
    public static final class Permit implements AutoCloseable {

        private final RmConcurrencyLimiter limiter;
        private final List<AcquiredSlot> acquiredSlots;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private Permit(RmConcurrencyLimiter limiter, List<AcquiredSlot> acquiredSlots) {
            this.limiter = limiter;
            this.acquiredSlots = acquiredSlots;
        }

        @Override
        public void close() {
            if (limiter != null && !acquiredSlots.isEmpty() && released.compareAndSet(false, true)) {
                limiter.releaseAll(acquiredSlots);
            }
        }
    }
}
