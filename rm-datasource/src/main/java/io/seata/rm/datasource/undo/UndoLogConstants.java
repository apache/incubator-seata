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
package io.seata.rm.datasource.undo;

import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;

import static io.seata.common.DefaultValues.DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION;

/**
 */
public interface UndoLogConstants {

    String SERIALIZER_KEY = "serializer";

    String DEFAULT_SERIALIZER = ConfigurationFactory.getInstance()
        .getConfig(ConfigurationKeys.TRANSACTION_UNDO_LOG_SERIALIZATION, DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION);

    String COMPRESSOR_TYPE_KEY = "compressorType";
}
