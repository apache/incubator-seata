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

package com.alibaba.fescar.server.coordinator;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackResponse;
import com.alibaba.fescar.core.rpc.RpcContext;
import com.alibaba.fescar.server.event.EventBusManager;
import com.alibaba.fescar.server.session.SessionHolder;

public class DefaultCoreForEventBusTest {
  @Test
  public void test() throws IOException, TransactionException, InterruptedException {
    SessionHolder.init(null);
    DefaultCoordinator coordinator = new DefaultCoordinator(null);
    coordinator.init();

    GlobalTransactionEventListener listener = new GlobalTransactionEventListener();
    EventBusManager.get().register(listener);

    //start a transaction
    GlobalBeginRequest request = new GlobalBeginRequest();
    request.setTransactionName("test_transaction");
    GlobalBeginResponse response = new GlobalBeginResponse();
    coordinator.doGlobalBegin(request, response, new RpcContext());

    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.Begin).get());

    //commit this transaction
    GlobalCommitRequest commitRequest = new GlobalCommitRequest();
    commitRequest.setTransactionId(XID.getTransactionId(response.getXid()));
    coordinator.doGlobalCommit(commitRequest, new GlobalCommitResponse(), new RpcContext());

    //we need sleep for a short while because default canBeCommittedAsync() is true
    Thread.sleep(50);

    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.AsyncCommitting).get());
    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.Committed).get());

    //start another new transaction
    request = new GlobalBeginRequest();
    request.setTransactionName("test_transaction_2");
    response = new GlobalBeginResponse();
    coordinator.doGlobalBegin(request, response, new RpcContext());

    Assert.assertEquals(2, listener.getEventCounters().get(GlobalStatus.Begin).get());

    //rollback this transaction
    GlobalRollbackRequest rollbackRequest = new GlobalRollbackRequest();
    rollbackRequest.setTransactionId(XID.getTransactionId(response.getXid()));
    coordinator.doGlobalRollback(rollbackRequest, new GlobalRollbackResponse(), new RpcContext());

    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.Rollbacking).get());
    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.Rollbacked).get());

    //start more one new transaction for test timeout
    request = new GlobalBeginRequest();
    //let this transaction immediately timeout
    request.setTimeout(0);
    request.setTransactionName("test_transaction_3");
    response = new GlobalBeginResponse();
    coordinator.doGlobalBegin(request, response, new RpcContext());
    //sleep for check ->  DefaultCoordinator.timeoutCheck
    Thread.sleep(10);

    //at lease retry once because DefaultCoordinator.timeoutCheck is 2 milliseconds
    Assert.assertTrue(listener.getEventCounters().get(GlobalStatus.TimeoutRollbacking).get() >= 1);

  }
}
