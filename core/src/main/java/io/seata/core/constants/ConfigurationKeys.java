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
package io.seata.core.constants;

/**
 * The type Configuration keys.
 *
 * @author jimin.jm @alibaba-inc.com
 */
public class ConfigurationKeys {

    /**
     * The constant SERVICE_PREFIX.
     */
    public static final String SERVICE_PREFIX = "service.";

    /**
     * The constant STORE_PREFIX.
     */
    public static final String STORE_PREFIX = "store.";

    /**
     * The constant STORE_MODE.
     */
    public static final String STORE_MODE =  STORE_PREFIX + "mode";

    /**
     * The constant STORE_FILE_PREFIX
     */
    public static final String STORE_FILE_PREFIX = STORE_PREFIX + "file.";

    /**
     * The constant STORE_FILE_DIR
     */
    public static final String STORE_FILE_DIR = STORE_FILE_PREFIX + "dir";

    /**
     * The constant SERVICE_GROUP_MAPPING_PREFIX.
     */
    public static final String SERVICE_GROUP_MAPPING_PREFIX = SERVICE_PREFIX + "vgroup_mapping.";
    /**
     * The constant GROUPLIST_POSTFIX.
     */
    public static final String GROUPLIST_POSTFIX = ".grouplist";
    /**
     * The constant SERVER_NODE_SPLIT_CHAR.
     */
    public static final String SERVER_NODE_SPLIT_CHAR = System.getProperty("line.separator");

    /**
     * The constant ENABLE_DEGRADE_POSTFIX.
     */
    public static final String ENABLE_DEGRADE_POSTFIX = "enableDegrade";

    /**
     * The constant CLIENT_PREFIX.
     */
    public static final String CLIENT_PREFIX = "client.";

    /**
     * The constant CLIENT_ASYNC_COMMIT_BUFFER_LIMIT.
     */
    public static final String CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = CLIENT_PREFIX + "async.commit.buffer.limit";
    /**
     * The constant CLIENT_LOCK_RETRY_TIMES.
     */
    public static final String CLIENT_LOCK_RETRY_TIMES = CLIENT_PREFIX + "lock.retry.times";
    /**
     * The constant CLIENT_LOCK_RETRY_INTERNAL.
     */
    public static final String CLIENT_LOCK_RETRY_INTERNAL = CLIENT_PREFIX + "lock.retry.internal";

    /**
     * The constant SERVICE_SESSION_RELOAD_READ_SIZE
     */
    public static final String SERVICE_SESSION_RELOAD_READ_SIZE = STORE_FILE_PREFIX + "session.reload.read_size";

    /**
     * The constant CLIENT_REPORT_RETRY_COUNT.
     */
    public static final String CLIENT_REPORT_RETRY_COUNT = CLIENT_PREFIX + "report.retry.count";
}
