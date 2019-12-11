/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.metrics;

/**
 * Seata metrics constants for id
 *
 * @author zhengyangyong
 */
public class IdConstants {
    public static final String SEATA_TRANSACTION = "seata.transaction";

    public static final String NAME_KEY = "name";

    public static final String ROLE_KEY = "role";

    public static final String METER_KEY = "meter";

    public static final String STATISTIC_KEY = "statistic";

    public static final String STATUS_KEY = "status";

    public static final String ROLE_VALUE_TC = "tc";

    public static final String ROLE_VALUE_TM = "tm";

    public static final String ROLE_VALUE_RM = "rm";

    public static final String METER_VALUE_GAUGE = "gauge";

    public static final String METER_VALUE_COUNTER = "counter";

    public static final String METER_VALUE_SUMMARY = "summary";

    public static final String METER_VALUE_TIMER = "timer";

    public static final String STATISTIC_VALUE_COUNT = "count";

    public static final String STATISTIC_VALUE_TOTAL = "total";

    public static final String STATISTIC_VALUE_TPS = "tps";

    public static final String STATISTIC_VALUE_MAX = "max";

    public static final String STATISTIC_VALUE_AVERAGE = "average";

    public static final String STATUS_VALUE_ACTIVE = "active";

    public static final String STATUS_VALUE_COMMITTED = "committed";

    public static final String STATUS_VALUE_ROLLBACKED = "rollbacked";
}
