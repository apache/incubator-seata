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
package org.apache.seata.server.coordinator;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequest;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.store.StoreConfig;
import org.springframework.context.ApplicationListener;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type raft tx coordinator.
 */
public class RaftCoordinator extends DefaultCoordinator implements ApplicationListener<ClusterChangeEvent> {

    protected static final Map<String, Boolean> GROUP_PREVENT = new ConcurrentHashMap<>();

    public RaftCoordinator(RemotingServer remotingServer) {
        super(remotingServer);
    }

    @Override
    public <T extends AbstractTransactionRequest, S extends AbstractTransactionResponse> void exceptionHandleTemplate(Callback<T, S> callback, T request, S response) {
        String group = SeataClusterContext.bindGroup();
        try {
            if (!isPass(group)) {
                throw new TransactionException(TransactionExceptionCode.NotRaftLeader,
                        " The current TC is not a leader node, interrupt processing !");
            }
            super.exceptionHandleTemplate(callback,request,response);
        } catch (TransactionException tex) {
            LOGGER.error("Catch TransactionException while do RPC, request: {}", request, tex);
            callback.onTransactionException(request, response, tex);
        } finally {
            SeataClusterContext.unbindGroup();
        }
    }

    private boolean isPass(String group) {
        // Non-raft mode always allows requests
        return Optional.ofNullable(GROUP_PREVENT.get(group)).orElse(false);
    }

    public static void setPrevent(String group, boolean prevent) {
        if (StoreConfig.getSessionMode() == StoreConfig.SessionMode.RAFT) {
            GROUP_PREVENT.put(group, prevent);
        }
    }


    @Override
    public void onApplicationEvent(ClusterChangeEvent event) {
        setPrevent(event.getGroup(), event.isLeader());
    }

}
