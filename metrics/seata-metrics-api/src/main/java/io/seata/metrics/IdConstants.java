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
public interface IdConstants {
    String SEATA_TRANSACTION = "seata.transaction";

    String APP_ID_KEY = "applicationId";
    
    String GROUP_KEY = "group";

    String NAME_KEY = "name";

    String ROLE_KEY = "role";

    String METER_KEY = "meter";

    String STATISTIC_KEY = "statistic";

    String STATUS_KEY = "status";

    String ROLE_VALUE_TC = "tc";

    String ROLE_VALUE_TM = "tm";

    String ROLE_VALUE_RM = "rm";

    String METER_VALUE_GAUGE = "gauge";

    String METER_VALUE_COUNTER = "counter";

    String METER_VALUE_SUMMARY = "summary";

    String METER_VALUE_TIMER = "timer";

    String STATISTIC_VALUE_COUNT = "count";

    String STATISTIC_VALUE_TOTAL = "total";

    String STATISTIC_VALUE_TPS = "tps";

    String STATISTIC_VALUE_MAX = "max";

    String STATISTIC_VALUE_AVERAGE = "average";

    String STATUS_VALUE_ACTIVE = "active";

    String STATUS_VALUE_COMMITTED = "committed";

    String STATUS_VALUE_ROLLBACKED = "rollbacked";

    String STATUS_VALUE_FAILED = "failed";

    String STATUS_VALUE_TWO_PHASE_TIMEOUT = "2phaseTimeout";

    String RETRY_KEY = "retry";

    String STATUS_VALUE_AFTER_COMMITTED_KEY = "AfterCommitted";

    String STATUS_VALUE_AFTER_ROLLBACKED_KEY = "AfterRollbacked";

}
