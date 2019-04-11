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

public class GuavaEventBus implements EventBus {
  private final com.google.common.eventbus.EventBus eventBus;

  public GuavaEventBus(String identifier) {
    this.eventBus = new com.google.common.eventbus.EventBus(identifier);
  }

  @Override
  public void register(Object object) {
    this.eventBus.register(object);
  }

  @Override
  public void unregister(Object object) {
    this.eventBus.unregister(object);
  }

  @Override
  public void post(Object event) {
    this.eventBus.post(event);
  }
}
