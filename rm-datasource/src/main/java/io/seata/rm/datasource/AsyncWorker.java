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
package io.seata.rm.datasource;

import com.google.common.collect.Lists;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.IOUtil;
import io.seata.config.ConfigurationFactory;
import io.seata.core.model.BranchStatus;
import io.seata.rm.datasource.undo.UndoLogManager;
import io.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static io.seata.common.DefaultValues.DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;
import static io.seata.core.constants.ConfigurationKeys.CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;

/**
 * The type Async worker.
 *
 * @author sharajava
 */
public class AsyncWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncWorker.class);

    private static final int DEFAULT_RESOURCE_SIZE = 16;

    private static final int UNDOLOG_DELETE_LIMIT_SIZE = 1000;

    private static final int ASYNC_COMMIT_BUFFER_LIMIT = ConfigurationFactory.getInstance().getInt(
            CLIENT_ASYNC_COMMIT_BUFFER_LIMIT, DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT);

    private final DataSourceManager dataSourceManager;

    private final BlockingQueue<BranchPhaseContext> undoLogQueue;

    private final ScheduledExecutorService scheduledExecutor;

    private static volatile AsyncWorker instance = null;

    public AsyncWorker(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;

        LOGGER.info("Async Commit Buffer Limit: {}", ASYNC_COMMIT_BUFFER_LIMIT);
        undoLogQueue = new LinkedBlockingQueue<>(ASYNC_COMMIT_BUFFER_LIMIT);

        ThreadFactory threadFactory = new NamedThreadFactory("AsyncWorker", 2, true);
        scheduledExecutor = new ScheduledThreadPoolExecutor(2, threadFactory);
        scheduledExecutor.scheduleAtFixedRate(this::doBranchCommitSafely, 10, 1000, TimeUnit.MILLISECONDS);
    }

    public static AsyncWorker getInstance(DataSourceManager dataSourceManager) {
        if (instance == null) {
            synchronized (AsyncWorker.class) {
                if (instance == null) {
                    instance = new AsyncWorker(dataSourceManager);
                }
            }
        }
        return instance;
    }


    public BranchStatus branchCommit(String xid, long branchId, String resourceId) {
        BranchPhaseContext context = new BranchPhaseContext(xid, branchId, resourceId);
        addToUndoLogQueue(context);
        return BranchStatus.PhaseTwo_Committed;
    }

    public void cleanSuspendUndoLog(final String xid, final long branchId, String resourceId) {
        BranchPhaseContext context = new BranchPhaseContext(xid, branchId, resourceId);
        addToUndoLogQueue(context);
    }

    /**
     * try add context to commitQueue directly, if fail(which means the queue is full),
     * then doBranchCommit urgently(so that the queue could be empty again) and retry this process.
     */
    private void addToUndoLogQueue(BranchPhaseContext context) {
        if (undoLogQueue.offer(context)) {
            return;
        }
        CompletableFuture.runAsync(this::doBranchCommitSafely, scheduledExecutor)
                .thenRun(() -> addToUndoLogQueue(context));
    }

    private void addAllToUndoLogQueue(List<BranchPhaseContext> contexts) {
        for (BranchPhaseContext context : contexts) {
            addToUndoLogQueue(context);
        }
    }

    void doBranchCommitSafely() {
        try {
            doBranchCommit();
        } catch (Throwable e) {
            LOGGER.error("Exception occur when doing branch commit", e);
        }
    }

    private void doBranchCommit() {
        if (undoLogQueue.isEmpty()) {
            return;
        }

        // transfer all context currently received to this list
        List<BranchPhaseContext> allContexts = new LinkedList<>();
        undoLogQueue.drainTo(allContexts);

        // group context by their resourceId
        Map<String, List<BranchPhaseContext>> groupedContexts = groupedByResourceId(allContexts);

        groupedContexts.forEach(this::dealWithGroupedContexts);
    }

    Map<String, List<BranchPhaseContext>> groupedByResourceId(List<BranchPhaseContext> contexts) {
        Map<String, List<BranchPhaseContext>> groupedContexts = new HashMap<>(DEFAULT_RESOURCE_SIZE);
        contexts.forEach(context -> {
            List<BranchPhaseContext> group = groupedContexts.computeIfAbsent(context.resourceId, key -> new LinkedList<>());
            group.add(context);
        });
        return groupedContexts;
    }

    private void dealWithGroupedContexts(String resourceId, List<BranchPhaseContext> contexts) {
        DataSourceProxy dataSourceProxy = dataSourceManager.get(resourceId);
        if (dataSourceProxy == null) {
            LOGGER.warn("failed to find resource for {} and requeue", resourceId);
            addAllToUndoLogQueue(contexts);
            return;
        }

        Connection conn = null;
        try {
            conn = dataSourceProxy.getPlainConnection();
            UndoLogManager undoLogManager = UndoLogManagerFactory.getUndoLogManager(dataSourceProxy.getDbType());

            // split contexts into several lists, with each list contain no more element than limit size
            List<List<BranchPhaseContext>> splitByLimit = Lists.partition(contexts, UNDOLOG_DELETE_LIMIT_SIZE);
            for (List<BranchPhaseContext> partition : splitByLimit) {
                deleteUndoLog(conn, undoLogManager, partition);
            }
        } catch (SQLException sqlExx) {
            addAllToUndoLogQueue(contexts);
            LOGGER.error("failed to get connection for async committing on {} and requeue", resourceId, sqlExx);
        } finally {
            IOUtil.close(conn);
        }

    }

    private void deleteUndoLog(final Connection conn, UndoLogManager undoLogManager, List<BranchPhaseContext> contexts) {
        Set<String> xids = new LinkedHashSet<>(contexts.size());
        Set<Long> branchIds = new LinkedHashSet<>(contexts.size());
        contexts.forEach(context -> {
            xids.add(context.xid);
            branchIds.add(context.branchId);
        });

        try {
            undoLogManager.batchDeleteUndoLog(xids, branchIds, conn);
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to batch delete undo log", e);
            try {
                conn.rollback();
                addAllToUndoLogQueue(contexts);
            } catch (SQLException rollbackEx) {
                LOGGER.error("Failed to rollback JDBC resource after deleting undo log failed", rollbackEx);
            }
        }
    }

    static class BranchPhaseContext {

        /**
         * AT Phase 2 context
         *
         * @param xid        the xid
         * @param branchId   the branch id
         * @param resourceId the resource id
         */
        public BranchPhaseContext(String xid, long branchId, String resourceId) {
            this.xid = xid;
            this.branchId = branchId;
            this.resourceId = resourceId;
        }

        /**
         * The Xid.
         */
        String xid;
        /**
         * The Branch id.
         */
        long branchId;
        /**
         * The Resource id.
         */
        String resourceId;
    }
}
