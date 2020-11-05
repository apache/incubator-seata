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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.config.ConfigurationFactory;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.ResourceManagerInbound;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.undo.UndoLogManager;
import io.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.constants.ConfigurationKeys.CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;

/**
 * The type Async worker.
 *
 * @author sharajava
 */
public class AsyncWorker implements ResourceManagerInbound {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncWorker.class);

    private static final int DEFAULT_RESOURCE_SIZE = 16;

    private static final int UNDOLOG_DELETE_LIMIT_SIZE = 1000;

    private static final int ASYNC_COMMIT_BUFFER_LIMIT = ConfigurationFactory.getInstance().getInt(
        CLIENT_ASYNC_COMMIT_BUFFER_LIMIT, DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT);

    private final BlockingQueue<Phase2Context> ASYNC_COMMIT_BUFFER = new LinkedBlockingQueue<>(ASYNC_COMMIT_BUFFER_LIMIT);

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) {
        if (!ASYNC_COMMIT_BUFFER.offer(new Phase2Context(branchType, xid, branchId, resourceId, applicationData))) {
            LOGGER.warn("Async commit buffer is FULL. Rejected branch [{}/{}] will be handled by housekeeping later.", branchId, xid);
        }
        return BranchStatus.PhaseTwo_Committed;
    }

    /**
     * Init worker thread to do branch commit
     */
    public void init() {
        LOGGER.info("Async Commit Buffer Limit: {}", ASYNC_COMMIT_BUFFER_LIMIT);
        ScheduledExecutorService timerExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("AsyncWorker", 1, true));
        timerExecutor.scheduleAtFixedRate(() -> {
            try {

                doBranchCommits();

            } catch (Throwable e) {
                LOGGER.info("Failed at async committing ... {}", e.getMessage());

            }
        }, 10, 1000, TimeUnit.MILLISECONDS);
    }

    private void doBranchCommits() {
        if (ASYNC_COMMIT_BUFFER.isEmpty()) {
            return;
        }

        // transfer all context currently received to this list
        List<Phase2Context> allContexts = new LinkedList<>();
        ASYNC_COMMIT_BUFFER.drainTo(allContexts);

        // group context by their resourceId
        Map<String, List<Phase2Context>> groupedContexts = groupedByResourceId(allContexts);

        groupedContexts.forEach(this::dealWithGroupedContexts);
    }

    Map<String, List<Phase2Context>> groupedByResourceId(List<Phase2Context> contexts) {
        Map<String, List<Phase2Context>> groupedContexts = new HashMap<>(DEFAULT_RESOURCE_SIZE);
        contexts.forEach(context -> {
            List<Phase2Context> group = groupedContexts.computeIfAbsent(context.resourceId, key -> new LinkedList<>());
            group.add(context);
        });
        return groupedContexts;
    }

    private void dealWithGroupedContexts(String resourceId, List<Phase2Context> contexts) {
        DataSourceManager resourceManager = (DataSourceManager) DefaultResourceManager.get().getResourceManager(BranchType.AT);
        DataSourceProxy dataSourceProxy = resourceManager.get(resourceId);
        if (dataSourceProxy == null) {
            LOGGER.warn("Failed to find resource for {}" + resourceId);
            return;
        }

        Connection conn;
        try {
            conn = dataSourceProxy.getPlainConnection();
        } catch (SQLException sqle) {
            LOGGER.warn("Failed to get connection for async committing on " + resourceId, sqle);
            return;
        }

        UndoLogManager undoLogManager = UndoLogManagerFactory.getUndoLogManager(dataSourceProxy.getDbType());

        // split contexts into several lists, with each list contain no more element than limit size
        List<List<Phase2Context>> splitByLimit = Lists.partition(contexts, UNDOLOG_DELETE_LIMIT_SIZE);
        splitByLimit.forEach(partition -> deleteUndoLog(conn, undoLogManager, partition));
    }

    private void deleteUndoLog(Connection conn, UndoLogManager undoLogManager, List<Phase2Context> contexts) {
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
            LOGGER.warn("Failed to batch delete undo log [" + branchIds + "/" + xids + "]", e);
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                LOGGER.warn("Failed to rollback JDBC resource while deleting undo_log ", rollbackEx);
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException closeEx) {
                LOGGER.warn("Failed to close JDBC resource while deleting undo_log ", closeEx);
            }
        }
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) {
        throw new NotSupportYetException();
    }

    static class Phase2Context {

        /**
         * Instantiates a new Phase 2 context.
         *
         * @param branchType      the branchType
         * @param xid             the xid
         * @param branchId        the branch id
         * @param resourceId      the resource id
         * @param applicationData the application data
         */
        public Phase2Context(BranchType branchType, String xid, long branchId, String resourceId,
                             String applicationData) {
            this.xid = xid;
            this.branchId = branchId;
            this.resourceId = resourceId;
            this.applicationData = applicationData;
            this.branchType = branchType;
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
        /**
         * The Application data.
         */
        String applicationData;

        /**
         * the branch Type
         */
        BranchType branchType;
    }
}
