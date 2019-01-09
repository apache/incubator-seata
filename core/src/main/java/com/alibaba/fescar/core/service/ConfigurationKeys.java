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

package com.alibaba.fescar.core.service;

public class ConfigurationKeys {

    public static final String SERVICE_PREFIX = "service.";
    public static final String SERVICE_GROUP_MAPPING_PREFIX = SERVICE_PREFIX + "vgroup_mapping.";
    public static final String GROUPLIST_POSTFIX = ".grouplist";
    public static final String SERVER_NODE_SPLIT_CHAR = "\n";

    public static final String ENABLE_DEGRADE_POSTFIX = "enableDegrade";

    public static final String CLIENT_PREFIX = "client.";

    public static final String CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = CLIENT_PREFIX + "async.commit.buffer.limit";
    public static final String CLIENT_LOCK_RETRY_TIMES = CLIENT_PREFIX + "lock.retry.times";
    public static final String CLIENT_LOCK_RETRY_INTERNAL = CLIENT_PREFIX + "lock.retry.internal";
}
