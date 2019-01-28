/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.thread.NamedThreadFactory;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.ResourceManagerInbound;
import com.alibaba.fescar.rm.datasource.undo.UndoLogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.fescar.core.service.ConfigurationKeys.CLIENT_ASYNC_COMMIT_BUFFER_LIMIT;

/**
 * The type Async worker.
 */
public class AsyncWorker implements ResourceManagerInbound {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncWorker.class);

    private static class Phase2Context {

        /**
         * Instantiates a new Phase 2 context.
         *
         * @param xid             the xid
         * @param branchId        the branch id
         * @param resourceId      the resource id
         * @param applicationData the application data
         */
        public Phase2Context(String xid, long branchId, String resourceId, String applicationData) {
            this.xid = xid;
            this.branchId = branchId;
            this.resourceId = resourceId;
            this.applicationData = applicationData;
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
    }

    private static final List<Phase2Context> ASYNC_COMMIT_BUFFER = Collections.synchronizedList(new ArrayList<Phase2Context>());

    private static int ASYNC_COMMIT_BUFFER_LIMIT = ConfigurationFactory.getInstance().getInt(
        CLIENT_ASYNC_COMMIT_BUFFER_LIMIT, 10000);

    private static ScheduledExecutorService timerExecutor;

    @Override
    public BranchStatus branchCommit(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        if (ASYNC_COMMIT_BUFFER.size() < ASYNC_COMMIT_BUFFER_LIMIT) {
            ASYNC_COMMIT_BUFFER.add(new Phase2Context(xid, branchId, resourceId, applicationData));
        } else {
            LOGGER.warn("Async commit buffer is FULL. Rejected branch [" + branchId + "/" + xid + "] will be handled by housekeeping later.");
        }
        return BranchStatus.PhaseTwo_Committed;
    }

    /**
     * Init.
     */
    public synchronized void init() {
        LOGGER.info("Async Commit Buffer Limit: " + ASYNC_COMMIT_BUFFER_LIMIT);
        timerExecutor = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("AsyncWorker", 1, true));
        timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {

                    doBranchCommits();

                } catch (Throwable e) {
                    LOGGER.info("Failed at async committing ... " + e.getMessage());

                }
            }
        }, 10, 1000 * 1, TimeUnit.MILLISECONDS);
    }

    private void doBranchCommits() {
        if (ASYNC_COMMIT_BUFFER.size() == 0) {
            return;
        }
        Map<String, List<Phase2Context>> mappedContexts = new HashMap<>();
        Iterator<Phase2Context> iterator = ASYNC_COMMIT_BUFFER.iterator();
        while (iterator.hasNext()) {
            Phase2Context commitContext = iterator.next();
            List<Phase2Context> contextsGroupedByResourceId = mappedContexts.get(commitContext.resourceId);
            if (contextsGroupedByResourceId == null) {
                contextsGroupedByResourceId = new ArrayList<>();
                mappedContexts.put(commitContext.resourceId, contextsGroupedByResourceId);
            }
            contextsGroupedByResourceId.add(commitContext);

            iterator.remove();

        }

        for (String resourceId : mappedContexts.keySet()) {
            Connection conn = null;
            try {
                try {
                    DataSourceProxy dataSourceProxy = DataSourceManager.get().get(resourceId);
                    conn = dataSourceProxy.getPlainConnection();
                } catch (SQLException sqle) {
                    LOGGER.warn("Failed to get connection for async committing on " + resourceId, sqle);
                    continue;
                }

                List<Phase2Context> contextsGroupedByResourceId = mappedContexts.get(resourceId);
                for (Phase2Context commitContext : contextsGroupedByResourceId) {
                    try {
                        UndoLogManager.deleteUndoLog(commitContext.xid, commitContext.branchId, conn);
                    } catch (Exception ex) {
                        LOGGER.warn("Failed to delete undo log [" + commitContext.branchId + "/" + commitContext.xid + "]", ex);
                    }
                }

            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException closeEx) {
                        LOGGER.warn("Failed to close JDBC resource while deleting undo_log ", closeEx);
                    }
                }
            }

        }

    }

    @Override
    public BranchStatus branchRollback(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        throw new NotSupportYetException();

    }
}
