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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import io.seata.common.exception.FrameworkException;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.executor.Initialize;
import io.seata.common.util.NetUtil;
import io.seata.core.context.RootContext;
import io.seata.core.exception.RmTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.logger.StackTraceLogger;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManagerInbound;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.rpc.netty.NettyClientConfig;
import io.seata.core.rpc.netty.RmRpcClient;
import io.seata.core.rpc.netty.TmRpcClient;
import io.seata.discovery.loadbalance.LoadBalanceFactory;
import io.seata.discovery.registry.RegistryFactory;
import io.seata.rm.AbstractResourceManager;
import io.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.exception.FrameworkErrorCode.NoAvailableService;

/**
 * The type Data source manager.
 *
 * @author sharajava
 */
public class DataSourceManager extends AbstractResourceManager implements Initialize {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceManager.class);

    private ResourceManagerInbound asyncWorker;

    private Map<String, Resource> dataSourceCache = new ConcurrentHashMap<>();

    /**
     * Sets async worker.
     *
     * @param asyncWorker the async worker
     */
    public void setAsyncWorker(ResourceManagerInbound asyncWorker) {
        this.asyncWorker = asyncWorker;
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
        throws TransactionException {
        try {
            GlobalLockQueryRequest request = new GlobalLockQueryRequest();
            request.setXid(xid);
            request.setLockKey(lockKeys);
            request.setResourceId(resourceId);

            GlobalLockQueryResponse response = null;
            if (RootContext.inGlobalTransaction()) {
                response = (GlobalLockQueryResponse)RmRpcClient.getInstance().sendMsgWithResponse(request);
            } else if (RootContext.requireGlobalLock()) {
                response = (GlobalLockQueryResponse)RmRpcClient.getInstance().sendMsgWithResponse(loadBalance(),
                    request, NettyClientConfig.getRpcRequestTimeout());
            } else {
                throw new RuntimeException("unknow situation!");
            }

            if (response.getResultCode() == ResultCode.Failed) {
                throw new TransactionException(response.getTransactionExceptionCode(),
                    "Response[" + response.getMsg() + "]");
            }
            return response.isLockable();
        } catch (TimeoutException toe) {
            throw new RmTransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            throw new RmTransactionException(TransactionExceptionCode.LockableCheckFailed, "Runtime", rex);
        }

    }

    @SuppressWarnings("unchecked")
    private String loadBalance() {
        InetSocketAddress address = null;
        try {
            List<InetSocketAddress> inetSocketAddressList = RegistryFactory.getInstance().lookup(
                TmRpcClient.getInstance().getTransactionServiceGroup());
            address = LoadBalanceFactory.getInstance().select(inetSocketAddressList);
        } catch (Exception ignore) {
            LOGGER.error(ignore.getMessage());
        }
        if (address == null) {
            throw new FrameworkException(NoAvailableService);
        }
        return NetUtil.toStringAddress(address);
    }

    /**
     * Init.
     *
     * @param asyncWorker the async worker
     */
    public synchronized void initAsyncWorker(ResourceManagerInbound asyncWorker) {
        setAsyncWorker(asyncWorker);
    }

    /**
     * Instantiates a new Data source manager.
     */
    public DataSourceManager() {
    }

    @Override
    public void init() {
        AsyncWorker asyncWorker = new AsyncWorker();
        asyncWorker.init();
        initAsyncWorker(asyncWorker);
    }

    @Override
    public void registerResource(Resource resource) {
        DataSourceProxy dataSourceProxy = (DataSourceProxy)resource;
        dataSourceCache.put(dataSourceProxy.getResourceId(), dataSourceProxy);
        super.registerResource(dataSourceProxy);
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
        return (DataSourceProxy)dataSourceCache.get(resourceId);
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {
        return asyncWorker.branchCommit(branchType, xid, branchId, resourceId, applicationData);
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        DataSourceProxy dataSourceProxy = get(resourceId);
        if (dataSourceProxy == null) {
            throw new ShouldNeverHappenException();
        }
        try {
            UndoLogManagerFactory.getUndoLogManager(dataSourceProxy.getDbType()).undo(dataSourceProxy, xid, branchId);
        } catch (TransactionException te) {
            StackTraceLogger.info(LOGGER, te,
                "[stacktrace]branchRollback failed. branchType:[{}], xid:[{}], branchId:[{}], resourceId:[{}], applicationData:[{}]. stacktrace:[{}]",
                new Object[]{branchType, xid, branchId, resourceId, applicationData, te.getMessage()},
                "branchRollback failed reason [{}]", new Object[]{te.getMessage()});
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

}
