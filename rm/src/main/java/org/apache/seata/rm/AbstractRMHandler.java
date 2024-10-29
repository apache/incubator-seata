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
package org.apache.seata.rm;

import io.netty.channel.Channel;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.auth.JwtAuthManager;
import org.apache.seata.core.auth.RegisterHandler;
import org.apache.seata.core.exception.AbstractExceptionHandler;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.core.protocol.*;
import org.apache.seata.core.protocol.transaction.*;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * The Abstract RM event handler
 *
 */
public abstract class AbstractRMHandler extends AbstractExceptionHandler
        implements RMInboundHandler, TransactionMessageHandler, RegisterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRMHandler.class);

    @Override
    public BranchCommitResponse handle(BranchCommitRequest request) {
        BranchCommitResponse response = new BranchCommitResponse();
        exceptionHandleTemplate(new AbstractCallback<BranchCommitRequest, BranchCommitResponse>() {
            @Override
            public void execute(BranchCommitRequest request, BranchCommitResponse response)
                    throws TransactionException {
                doBranchCommit(request, response);
            }
        }, request, response);
        return response;
    }

    @Override
    public BranchRollbackResponse handle(BranchRollbackRequest request) {
        BranchRollbackResponse response = new BranchRollbackResponse();
        exceptionHandleTemplate(new AbstractCallback<BranchRollbackRequest, BranchRollbackResponse>() {
            @Override
            public void execute(BranchRollbackRequest request, BranchRollbackResponse response)
                    throws TransactionException {
                doBranchRollback(request, response);
            }
        }, request, response);
        return response;
    }

    /**
     * delete undo log
     * @param request the request
     */
    @Override
    public void handle(UndoLogDeleteRequest request) {
        // https://github.com/seata/seata/issues/2226
    }

    /**
     * Do branch commit.
     *
     * @param request  the request
     * @param response the response
     * @throws TransactionException the transaction exception
     */
    protected void doBranchCommit(BranchCommitRequest request, BranchCommitResponse response)
            throws TransactionException {
        String xid = request.getXid();
        long branchId = request.getBranchId();
        String resourceId = request.getResourceId();
        String applicationData = request.getApplicationData();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Branch committing: " + xid + " " + branchId + " " + resourceId + " " + applicationData);
        }
        BranchStatus status = getResourceManager().branchCommit(request.getBranchType(), xid, branchId, resourceId,
                applicationData);
        response.setXid(xid);
        response.setBranchId(branchId);
        response.setBranchStatus(status);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Branch commit result: " + status);
        }

    }

    /**
     * Do branch rollback.
     *
     * @param request  the request
     * @param response the response
     * @throws TransactionException the transaction exception
     */
    protected void doBranchRollback(BranchRollbackRequest request, BranchRollbackResponse response)
            throws TransactionException {
        String xid = request.getXid();
        long branchId = request.getBranchId();
        String resourceId = request.getResourceId();
        String applicationData = request.getApplicationData();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Branch Rollbacking: " + xid + " " + branchId + " " + resourceId);
        }
        BranchStatus status = getResourceManager().branchRollback(request.getBranchType(), xid, branchId, resourceId,
                applicationData);
        response.setXid(xid);
        response.setBranchId(branchId);
        response.setBranchStatus(status);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Branch Rollbacked result: " + status);
        }
    }

    /**
     * get resource manager implement
     *
     * @return resource manager
     */
    protected abstract ResourceManager getResourceManager();

    @Override
    public AbstractResultMessage onRequest(AbstractMessage request, RpcContext context) {
        if (!(request instanceof AbstractTransactionRequestToRM)) {
            throw new IllegalArgumentException();
        }
        AbstractTransactionRequestToRM transactionRequest = (AbstractTransactionRequestToRM)request;
        transactionRequest.setRMInboundMessageHandler(this);

        return transactionRequest.handle(context);
    }

    @Override
    public void onResponse(AbstractResultMessage response, RpcContext context) {
        LOGGER.info("the rm client received response msg [{}] from tc server.", response.toString());
    }

    @Override
    public void onRegisterResponse(RegisterRMResponse response, Channel channel, Integer rpcId) {
        LOGGER.info("the rm client received register response msg [{}] from tc server.", response.toString());
        try {
            JwtAuthManager authManager = JwtAuthManager.getInstance();
            ResultCode resultCode = response.getResultCode();
            RegisterRMRequest request = RmNettyRemotingClient.getInstance().getRegisterRMRequest(rpcId);
            HashMap<String, String> authMap = StringUtils.string2Map(request.getExtraData());
            boolean isTokenAuthFailed = resultCode.equals(ResultCode.Failed) &&
                    (authMap.containsKey(JwtAuthManager.PRO_TOKEN) || authMap.containsKey(JwtAuthManager.PRO_REFRESH_TOKEN));
            if (resultCode.equals(ResultCode.AccessTokenExpired)) {
                // refresh token to get access token
                authManager.setAccessToken(null);
                String identifyExtraData = authManager.getAuthData();
                request.setExtraData(identifyExtraData);
                RmNettyRemotingClient.getInstance().sendAsyncRequest(channel, request);
            } else if (resultCode.equals(ResultCode.RefreshTokenExpired) || isTokenAuthFailed) {
                // relogin to get refresh token and access token
                authManager.setAccessToken(null);
                authManager.setRefreshToken(null);
                String identifyExtraData = authManager.getAuthData();
                request.setExtraData(identifyExtraData);
                RmNettyRemotingClient.getInstance().sendAsyncRequest(channel, request);
            } else if (resultCode.equals(ResultCode.AccessTokenNearExpiration)) {
                //
                authManager.setAccessTokenNearExpiration(true);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("register RM success. client version:{}, server version:{},channel:{}",
                            request.getVersion(), request.getVersion(), channel);
                }

            } else if (resultCode.equals(ResultCode.Success)) {
                RmNettyRemotingClient.getInstance().refreshAuthToken(response.getExtraData());
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("register RM success. client version:{}, server version:{},channel:{}",
                            request.getVersion(), request.getVersion(), channel);
                }
                RmNettyRemotingClient.getInstance().removeRegisterRMRequest(rpcId);
            } else {
                String errMsg = String.format(
                        "register RM failed. client version: %s,server version: %s, errorMsg: %s, " + "channel: %s",
                        request.getVersion(), request.getVersion(), response.getMsg(), channel);
                RmNettyRemotingClient.getInstance().removeRegisterRMRequest(rpcId);
                throw new FrameworkException(errMsg);
            }
        } catch (Exception exx) {
            throw new FrameworkException(
                    "register RM" + " error, errMsg:" + exx.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("register RM success");
        }
    }

    public abstract BranchType getBranchType();
}
