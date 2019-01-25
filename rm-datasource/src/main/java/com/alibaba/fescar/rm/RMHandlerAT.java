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

package com.alibaba.fescar.rm;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.protocol.transaction.*;
import com.alibaba.fescar.core.protocol.transaction.BranchRollbackRequest;
import com.alibaba.fescar.core.rpc.TransactionMessageHandler;
import com.alibaba.fescar.rm.datasource.DataSourceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMHandlerAT extends AbstractRMHandlerAT implements RMInboundHandler, TransactionMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RMHandlerAT.class);

    private DataSourceManager dataSourceManager = DataSourceManager.get();

    @Override
    protected void doBranchCommit(BranchCommitRequest request, BranchCommitResponse response) throws TransactionException {
        String xid = request.getXid();
        long branchId = request.getBranchId();
        String resourceId = request.getResourceId();
        String applicationData = request.getApplicationData();
        LOGGER.info("AT Branch committing: " + xid + " " + branchId + " " + resourceId + " " + applicationData);
        BranchStatus status = dataSourceManager.branchCommit(xid, branchId, resourceId, applicationData);
        response.setBranchStatus(status);
        LOGGER.info("AT Branch commit result: " + status);

    }

    @Override
    protected void doBranchRollback(BranchRollbackRequest request, BranchRollbackResponse response) throws TransactionException {
        String xid = request.getXid();
        long branchId = request.getBranchId();
        String resourceId = request.getResourceId();
        String applicationData = request.getApplicationData();
        LOGGER.info("AT Branch rolling back: " + xid + " " + branchId + " " + resourceId);
        BranchStatus status = dataSourceManager.branchRollback(xid, branchId, resourceId, applicationData);
        response.setBranchStatus(status);
        LOGGER.info("AT Branch rollback result: " + status);

    }
}
