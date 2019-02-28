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

package com.alibaba.fescar.server.metrics;

import com.alibaba.fescar.metrics.Constants;
import com.alibaba.fescar.metrics.Id;

public class MeterIdConstants {
  public static final Id COUNTER_ACTIVE = new Id(Constants.FESCAR_TRANSACTION)
      .withTag(Constants.ROLE_KEY, Constants.ROLE_VALUE_TC)
      .withTag(Constants.METER_KEY, Constants.METER_VALUE_COUNTER)
      .withTag(Constants.STATUS_KEY, Constants.STATUS_VALUE_ACTIVE);

  public static final Id COUNTER_COMMITTED = new Id(Constants.FESCAR_TRANSACTION)
      .withTag(Constants.ROLE_KEY, Constants.ROLE_VALUE_TC)
      .withTag(Constants.METER_KEY, Constants.METER_VALUE_COUNTER)
      .withTag(Constants.STATUS_KEY, Constants.STATUS_VALUE_COMMITTED);

  public static final Id COUNTER_ROLLBACK = new Id(Constants.FESCAR_TRANSACTION)
      .withTag(Constants.ROLE_KEY, Constants.ROLE_VALUE_TC)
      .withTag(Constants.METER_KEY, Constants.METER_VALUE_COUNTER)
      .withTag(Constants.STATUS_KEY, Constants.STATUS_VALUE_ROLLBACK);

  public static final Id SUMMARY_COMMITTED = new Id(Constants.FESCAR_TRANSACTION)
      .withTag(Constants.ROLE_KEY, Constants.ROLE_VALUE_TC)
      .withTag(Constants.METER_KEY, Constants.METER_VALUE_SUMMARY)
      .withTag(Constants.STATUS_KEY, Constants.STATUS_VALUE_COMMITTED);

  public static final Id SUMMARY_ROLLBACK = new Id(Constants.FESCAR_TRANSACTION)
      .withTag(Constants.ROLE_KEY, Constants.ROLE_VALUE_TC)
      .withTag(Constants.METER_KEY, Constants.METER_VALUE_SUMMARY)
      .withTag(Constants.STATUS_KEY, Constants.STATUS_VALUE_ROLLBACK);

  public static final Id TIMER_COMMITTED = new Id(Constants.FESCAR_TRANSACTION)
      .withTag(Constants.ROLE_KEY, Constants.ROLE_VALUE_TC)
      .withTag(Constants.METER_KEY, Constants.METER_VALUE_TIMER)
      .withTag(Constants.STATUS_KEY, Constants.STATUS_VALUE_COMMITTED);

  public static final Id TIMER_ROLLBACK = new Id(Constants.FESCAR_TRANSACTION)
      .withTag(Constants.ROLE_KEY, Constants.ROLE_VALUE_TC)
      .withTag(Constants.METER_KEY, Constants.METER_VALUE_TIMER)
      .withTag(Constants.STATUS_KEY, Constants.STATUS_VALUE_ROLLBACK);
}
