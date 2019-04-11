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

package com.alibaba.fescar.core.event;

import org.junit.Assert;
import org.junit.Test;

public class GuavaEventBusTest {
  @Test
  public void test() {
    EventBus eventBus = new GuavaEventBus("test");
    TransactionEventListener listener = new TransactionEventListener();
    eventBus.register(listener);

    eventBus.post(new TransactionStartEvent(TransactionEvent.ROLE_TC,"test",System.currentTimeMillis()));
    eventBus.post(new TransactionCommitEvent(TransactionEvent.ROLE_TC,"test",System.currentTimeMillis(),System.currentTimeMillis()));
    eventBus.post(new TransactionRollbackEvent(TransactionEvent.ROLE_TC,"test",System.currentTimeMillis(),System.currentTimeMillis()));

    Assert.assertEquals(listener.getStartEventCount().get(),1);
    Assert.assertEquals(listener.getCommitEventCount().get(),1);
    Assert.assertEquals(listener.getRollbackEventCount().get(),1);

    eventBus.unregister(listener);

    eventBus.post(new TransactionStartEvent(TransactionEvent.ROLE_TC,"test",System.currentTimeMillis()));
    eventBus.post(new TransactionCommitEvent(TransactionEvent.ROLE_TC,"test",System.currentTimeMillis(),System.currentTimeMillis()));
    eventBus.post(new TransactionRollbackEvent(TransactionEvent.ROLE_TC,"test",System.currentTimeMillis(),System.currentTimeMillis()));

    Assert.assertEquals(listener.getStartEventCount().get(),1);
    Assert.assertEquals(listener.getCommitEventCount().get(),1);
    Assert.assertEquals(listener.getRollbackEventCount().get(),1);
  }
}
