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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.exception.TransactionExceptionCode;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.Resource;
import com.alibaba.fescar.core.model.ResourceManager;
import com.alibaba.fescar.core.model.ResourceManagerInbound;
import com.alibaba.fescar.core.protocol.ResultCode;
import com.alibaba.fescar.core.protocol.transaction.*;
import com.alibaba.fescar.core.protocol.transaction.BranchRegisterRequest;
import com.alibaba.fescar.core.rpc.netty.RmRpcClient;
import com.alibaba.fescar.rm.datasource.undo.UndoLogManager;

public class DataSourceManager implements ResourceManager {

    private ResourceManagerInbound asyncWorker;

    private Map<String, Resource> dataSourceCache = new ConcurrentHashMap<>();

    public void setAsyncWorker(ResourceManagerInbound asyncWorker) {
        this.asyncWorker = asyncWorker;
    }

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String lockKeys) throws TransactionException {
        try {
            BranchRegisterRequest request = new BranchRegisterRequest();
            request.setTransactionId(XID.getTransactionId(xid));
            request.setLockKey(lockKeys);
            request.setResourceId(resourceId);

            BranchRegisterResponse response = (BranchRegisterResponse) RmRpcClient.getInstance().sendMsgWithResponse(request);
            if (response.getResultCode() == ResultCode.Failed) {
                throw new TransactionException(response.getTransactionExceptionCode(), "Response[" + response.getMsg() + "]");
            }
            return response.getBranchId();
        } catch (TimeoutException toe) {
            throw new TransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            throw new TransactionException(TransactionExceptionCode.BranchRegisterFailed, "Runtime", rex);
        }
    }

    @Override
    public void branchReport(String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
        try {
            BranchReportRequest request = new BranchReportRequest();
            request.setTransactionId(XID.getTransactionId(xid));
            request.setBranchId(branchId);
            request.setStatus(status);
            request.setApplicationData(applicationData);

            BranchReportResponse response = (BranchReportResponse) RmRpcClient.getInstance().sendMsgWithResponse(request);
            if (response.getResultCode() == ResultCode.Failed) {
                throw new TransactionException(response.getTransactionExceptionCode(), "Response[" + response.getMsg() + "]");
            }
        } catch (TimeoutException toe) {
            throw new TransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            throw new TransactionException(TransactionExceptionCode.BranchReportFailed, "Runtime", rex);
        }

    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
        try {
            GlobalLockQueryRequest request = new GlobalLockQueryRequest();
            request.setTransactionId(XID.getTransactionId(xid));
            request.setLockKey(lockKeys);
            request.setResourceId(resourceId);

            GlobalLockQueryResponse response = (GlobalLockQueryResponse) RmRpcClient.getInstance().sendMsgWithResponse(request);
            if (response.getResultCode() == ResultCode.Failed) {
                throw new TransactionException(response.getTransactionExceptionCode(), "Response[" + response.getMsg() + "]");
            }
            return response.isLockable();
        } catch (TimeoutException toe) {
            throw new TransactionException(TransactionExceptionCode.IO, "RPC Timeout", toe);
        } catch (RuntimeException rex) {
            throw new TransactionException(TransactionExceptionCode.LockableCheckFailed, "Runtime", rex);
        }

    }

    private static class SingletonHolder {
        private static DataSourceManager INSTANCE = new DataSourceManager();
    }

    public static DataSourceManager get() {
        return SingletonHolder.INSTANCE;
    }

    public static void set(DataSourceManager mock) {
        SingletonHolder.INSTANCE = mock;
    }

    public static synchronized void init(ResourceManagerInbound asyncWorker) {
        get().setAsyncWorker(asyncWorker);
    }

    protected DataSourceManager() {
    }

    @Override
    public void registerResource(Resource resource) {
        DataSourceProxy dataSourceProxy = (DataSourceProxy) resource;
        dataSourceCache.put(dataSourceProxy.getResourceId(), dataSourceProxy);
        RmRpcClient.getInstance().registerResource(dataSourceProxy.getResourceGroupId(), dataSourceProxy.getResourceId());

    }

    @Override
    public void unregisterResource(Resource resource) {
        throw new NotSupportYetException("unregister a resource");
    }

    public DataSourceProxy get(String resourceId) {
        return (DataSourceProxy) dataSourceCache.get(resourceId);
    }

    @Override
    public BranchStatus branchCommit(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        return asyncWorker.branchCommit(xid, branchId, resourceId, applicationData);
    }

    @Override
    public BranchStatus branchRollback(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        DataSourceProxy dataSourceProxy = get(resourceId);
        if (dataSourceProxy == null) {
            throw new ShouldNeverHappenException();
        }
        try {
            UndoLogManager.undo(dataSourceProxy, xid, branchId);
        } catch (TransactionException te) {
            if (te.getCode() == TransactionExceptionCode.BranchRollbackFailed_Unretriable) {
                return BranchStatus.PhaseTwo_RollbackFailed_Unretriable;
            } else {
                return BranchStatus.PhaseTwo_RollbackFailed_Retriable;
            }
        }
        return BranchStatus.PhaseTwo_Rollbacked;

    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return dataSourceCache;
    }
}
