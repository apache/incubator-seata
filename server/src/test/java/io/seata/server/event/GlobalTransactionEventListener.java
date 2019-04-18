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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.Subscribe;

import io.seata.core.model.GlobalStatus;

public class GlobalTransactionEventListener {
  private final Map<GlobalStatus, AtomicInteger> eventCounters;

  public Map<GlobalStatus, AtomicInteger> getEventCounters() {
    return eventCounters;
  }

  public GlobalTransactionEventListener() {
    this.eventCounters = new ConcurrentHashMap<>();
  }

  @Subscribe
  public void processTransactionEvent(GlobalTransactionEvent event) {
    AtomicInteger counter = eventCounters.computeIfAbsent(event.getStatus(), status -> new AtomicInteger(0));
    counter.addAndGet(1);
  }
}
