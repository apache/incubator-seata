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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.common.util.NetUtil;
import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.common.executor.Initialize;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.exception.TransactionExceptionCode;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.Resource;
import com.alibaba.fescar.core.model.ResourceManager;
import com.alibaba.fescar.core.model.ResourceManagerInbound;
import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterResponse;
import com.alibaba.fescar.core.protocol.transaction.BranchReportRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchReportResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalLockQueryRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalLockQueryResponse;
import com.alibaba.fescar.core.rpc.netty.NettyClientConfig;
import com.alibaba.fescar.core.rpc.netty.RmRpcClient;
import com.alibaba.fescar.core.rpc.netty.TmRpcClient;
import com.alibaba.fescar.discovery.loadbalance.LoadBalanceFactory;
import com.alibaba.fescar.discovery.registry.RegistryFactory;
import com.alibaba.fescar.rm.AbstractResourceManager;
import com.alibaba.fescar.rm.datasource.undo.UndoLogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.fescar.common.exception.FrameworkErrorCode.NoAvailableService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * The type Data source manager.
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
            request.setTransactionId(XID.getTransactionId(xid));
            request.setLockKey(lockKeys);
            request.setResourceId(resourceId);
            request.setBranchType(branchType);

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
            throw new TransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            throw new TransactionException(TransactionExceptionCode.LockableCheckFailed, "Runtime", rex);
        }
    }

    /**
     * for test
     */
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

    private static class SingletonHolder {
        private static DataSourceManager INSTANCE = new DataSourceManager(true);
    }

    /**
     * Get data source manager.
     *
     * @return the data source manager
     */
    public static DataSourceManager get() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Set.
     *
     * @param mock the mock
     */
    public static void set(DataSourceManager mock) {
        SingletonHolder.INSTANCE = mock;
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

    public DataSourceManager(boolean isInit) {
        if(isInit){
            init();
        }

    }

    @Override
    public void init(){
        AsyncWorker asyncWorker = new AsyncWorker();
        asyncWorker.init();
        initAsyncWorker(asyncWorker);
    }

    @Override
    public void registerResource(Resource resource) {
        DataSourceProxy dataSourceProxy = (DataSourceProxy) resource;
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
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        return asyncWorker.branchCommit(branchType, xid, branchId, resourceId, applicationData);
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        DataSourceProxy dataSourceProxy = get(resourceId);
        if (dataSourceProxy == null) {
            throw new ShouldNeverHappenException();
        }
        try {
            UndoLogManager.undo(dataSourceProxy, xid, branchId);
        } catch (TransactionException te) {
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
    public BranchType getBranchType(){
        return BranchType.AT;
    }

}
