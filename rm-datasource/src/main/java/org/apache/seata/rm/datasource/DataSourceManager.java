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
package org.apache.seata.rm.datasource;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.pool.PoolManager;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.RmTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.logger.StackTraceLogger;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.apache.seata.rm.AbstractResourceManager;
import org.apache.seata.rm.datasource.pool.ConnectionPoolReporter;
import org.apache.seata.rm.datasource.pool.PoolConfigUpdateProcessor;
import org.apache.seata.rm.datasource.pool.PoolManagerFactory;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Data source manager.
 *
 */
public class DataSourceManager extends AbstractResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceManager.class);

    private final AsyncWorker asyncWorker = new AsyncWorker(this);

    private final Map<String, Resource> dataSourceCache = new ConcurrentHashMap<>();
    private final Configuration configuration = ConfigurationFactory.getInstance();
    private final boolean poolMonitorEnabled = configuration.getBoolean("client.rm.pool.monitor.enabled", false);
    private final long reportIntervalMs = configuration.getLong("client.rm.pool.monitor.report-interval", 15000L);
    private final ConcurrentHashMap<String, ConnectionPoolReporter> reporters = new ConcurrentHashMap<>();
    private static final AtomicBoolean CONFIG_PROCESSOR_REGISTERED = new AtomicBoolean(false);

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
            throws TransactionException {
        GlobalLockQueryRequest request = new GlobalLockQueryRequest();
        request.setXid(xid);
        request.setLockKey(lockKeys);
        request.setResourceId(resourceId);
        try {
            GlobalLockQueryResponse response;
            if (RootContext.inGlobalTransaction() || RootContext.requireGlobalLock()) {
                response = (GlobalLockQueryResponse)
                        RmNettyRemotingClient.getInstance().sendSyncRequest(request);
            } else {
                throw new RuntimeException("unknow situation!");
            }

            if (response.getResultCode() == ResultCode.Failed) {
                throw new TransactionException(
                        response.getTransactionExceptionCode(), "Response[" + response.getMsg() + "]");
            }
            return response.isLockable();
        } catch (TimeoutException toe) {
            throw new RmTransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            throw new RmTransactionException(TransactionExceptionCode.LockableCheckFailed, "Runtime", rex);
        }
    }

    private static final ExecutorService POOL_CFG_EXECUTOR = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new NamedThreadFactory("rm-pool-config-update", 1));

    /**
     * Instantiates a new Data source manager.
     */
    public DataSourceManager() {
        registerPoolConfigProcessorOnce();
    }

    private void registerPoolConfigProcessorOnce() {
        if (!CONFIG_PROCESSOR_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        RmNettyRemotingClient rmClient = RmNettyRemotingClient.getInstance();
        RemotingProcessor processor = new PoolConfigUpdateProcessor(rmClient);
        rmClient.registerProcessor(MessageType.TYPE_POOL_CONFIG_UPDATE, processor, POOL_CFG_EXECUTOR);
        LOGGER.info("Registered PoolConfigUpdateProcessor for messageType={}", MessageType.TYPE_POOL_CONFIG_UPDATE);
    }

    @Override
    public void registerResource(Resource resource) {
        DataSourceProxy dataSourceProxy = (DataSourceProxy) resource;
        dataSourceCache.put(dataSourceProxy.getResourceId(), dataSourceProxy);
        super.registerResource(dataSourceProxy);

        if (poolMonitorEnabled) {
            try {
                String serviceName = dataSourceProxy.getResourceId();
                PoolManager poolManager = PoolManagerFactory.create(dataSourceProxy, serviceName);
                ConnectionPoolReporter reporter = new ConnectionPoolReporter(serviceName, poolManager);
                reporter.start();
                reporters.put(serviceName, reporter);
                LOGGER.info("Started ConnectionPoolReporter for {}", serviceName);
            } catch (Throwable t) {
                LOGGER.warn("Start ConnectionPoolReporter failed for {}", dataSourceProxy.getResourceId(), t);
            }
        }
    }

    @Override
    public void unregisterResource(Resource resource) {
        throw new NotSupportYetException("unregister a resource");
    }

    /**
     * Get data source proxy.
     *
     * @param resourceId the resource id
     * @return the data source proxy
     */
    public DataSourceProxy get(String resourceId) {
        return (DataSourceProxy) dataSourceCache.get(resourceId);
    }

    @Override
    public BranchStatus branchCommit(
            BranchType branchType, String xid, long branchId, String resourceId, String applicationData)
            throws TransactionException {
        return asyncWorker.branchCommit(xid, branchId, resourceId);
    }

    @Override
    public BranchStatus branchRollback(
            BranchType branchType, String xid, long branchId, String resourceId, String applicationData)
            throws TransactionException {
        DataSourceProxy dataSourceProxy = get(resourceId);
        if (dataSourceProxy == null) {
            throw new ShouldNeverHappenException(String.format("resource: %s not found", resourceId));
        }
        try {
            UndoLogManagerFactory.getUndoLogManager(dataSourceProxy.getDbType()).undo(dataSourceProxy, xid, branchId);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("branch rollback success, xid:{}, branchId:{}", xid, branchId);
            }
        } catch (TransactionException te) {
            StackTraceLogger.error(
                    LOGGER,
                    te,
                    "branchRollback failed. branchType:[{}], xid:[{}], branchId:[{}], resourceId:[{}], applicationData:[{}]. reason:[{}]",
                    new Object[] {branchType, xid, branchId, resourceId, applicationData, te.getMessage()});
            if (te.getCode() == TransactionExceptionCode.BranchRollbackFailed_Unretriable) {
                return BranchStatus.PhaseTwo_RollbackFailed_Unretryable;
            } else {
                return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
            }
        }
        return BranchStatus.PhaseTwo_Rollbacked;
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return dataSourceCache;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.AT;
    }

    public void stopAllPoolReporters() {
        reporters.forEach((k, r) -> {
            try {
                r.stop();
            } catch (Throwable ignore) {

            }
        });
        reporters.clear();
    }
}
