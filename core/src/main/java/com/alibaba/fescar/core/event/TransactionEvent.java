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

public class TransactionEvent {
  public static final String ROLE_TC = "tc";

  public static final String ROLE_TM = "tm";

  public static final String ROLE_RM = "rm";

  private final String role;

  private final String name;

  public String getRole() {
    return role;
  }

  public String getName() {
    return name;
  }

  public TransactionEvent(String role, String name) {
    this.role = role;
    this.name = name;
  }
}
