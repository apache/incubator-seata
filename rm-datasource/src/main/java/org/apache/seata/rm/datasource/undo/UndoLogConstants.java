/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.undo;

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.IdConstants;

import static org.apache.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION;

public interface UndoLogConstants {

    String SERIALIZER_KEY = "serializer";

    String DEFAULT_SERIALIZER = ConfigurationFactory.getInstance()
            .getConfig(
                    ConfigurationKeys.TRANSACTION_UNDO_LOG_SERIALIZATION, DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION);

    String COMPRESSOR_TYPE_KEY = "compressorType";

    String SUB_ID_KEY = "subId";

    String BRANCH_ID_KEY = "branchId";

    String SUB_SPLIT_KEY = ",";

    String MAX_ALLOWED_PACKET = "map";
    
    Id SUMMARY_UNDO_LOG_SIZE = new Id("seata.undo.log")
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_SUMMARY)
            .withTag("type", "size");

    Id TIMER_UNDO_LOG_DELETE_LATENCY = new Id("seata.undo.log")
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_TIMER)
            .withTag("type", "delete_latency");

    Id COUNTER_UNDO_LOG_DELETE_COUNT = new Id("seata.undo.log")
            .withTag(IdConstants.ROLE_KEY, IdConstants.ROLE_VALUE_RM)
            .withTag(IdConstants.METER_KEY, IdConstants.METER_VALUE_COUNTER)
            .withTag("type", "delete_count");
}
