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

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.Subscribe;

public class TransactionEventListener {
  private final AtomicInteger startEventCount;
  private final AtomicInteger commitEventCount;
  private final AtomicInteger rollbackEventCount;

  public AtomicInteger getStartEventCount() {
    return startEventCount;
  }

  public AtomicInteger getCommitEventCount() {
    return commitEventCount;
  }

  public AtomicInteger getRollbackEventCount() {
    return rollbackEventCount;
  }

  public TransactionEventListener() {
    this.startEventCount = new AtomicInteger(0);
    this.commitEventCount = new AtomicInteger(0);
    this.rollbackEventCount = new AtomicInteger(0);
  }

  @Subscribe
  public void processTransactionStartEvent(TransactionStartEvent event) {
    startEventCount.addAndGet(1);
  }

  @Subscribe
  public void processTransactionCommitEvent(TransactionCommitEvent event) {
    commitEventCount.addAndGet(1);
  }

  @Subscribe
  public void processTransactionRollbackEvent(TransactionRollbackEvent event) {
    rollbackEventCount.addAndGet(1);
  }
}
