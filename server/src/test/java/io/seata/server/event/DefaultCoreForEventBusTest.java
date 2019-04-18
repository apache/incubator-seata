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

package io.seata.server.event;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.server.coordinator.Core;
import io.seata.server.coordinator.CoreFactory;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.session.SessionHolder;

public class DefaultCoreForEventBusTest {
  @Test
  public void test() throws IOException, TransactionException, InterruptedException {
    SessionHolder.init(null);
    DefaultCoordinator coordinator = new DefaultCoordinator(null);
    coordinator.init();

    Core core = CoreFactory.get();

    GlobalTransactionEventListener listener = new GlobalTransactionEventListener();
    EventBusManager.get().register(listener);

    //start a transaction
    String xid = core.begin("test_app_id", "default_group", "test_tran_name", 30000);

    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.Begin).get());

    //commit this transaction
    core.commit(xid);

    //we need sleep for a short while because default canBeCommittedAsync() is true
    Thread.sleep(50);

    //check
    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.AsyncCommitting).get());
    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.Committed).get());

    //start another new transaction
    xid = core.begin("test_app_id", "default_group", "test_tran_name2", 30000);

    Assert.assertEquals(2, listener.getEventCounters().get(GlobalStatus.Begin).get());

    core.rollback(xid);

    //check
    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.Rollbacking).get());
    Assert.assertEquals(1, listener.getEventCounters().get(GlobalStatus.Rollbacked).get());

    //start more one new transaction for test timeout and let this transaction immediately timeout
    xid = core.begin("test_app_id", "default_group", "test_tran_name3", 0);

    //sleep for check ->  DefaultCoordinator.timeoutCheck
    Thread.sleep(100);

    //at lease retry once because DefaultCoordinator.timeoutCheck is 2 milliseconds
    Assert.assertTrue(listener.getEventCounters().get(GlobalStatus.TimeoutRollbacking).get() >= 1);
  }
}
