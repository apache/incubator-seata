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
package io.seata.rm.tcc.constant;

/**
 * TCC Fence Constant
 *
 * @author kaka2code
 */
public class TCCFenceConstant {

    /**
     * PHASE 1: The Commit tried.
     */
    public static final int STATUS_TRIED = 1;

    /**
     * PHASE 2: The Committed.
     */
    public static final int STATUS_COMMITTED = 2;

    /**
     * PHASE 2: The Rollbacked.
     */
    public static final int STATUS_ROLLBACKED = 3;

    /**
     * Suspended status.
     */
    public static final int STATUS_SUSPENDED = 4;

    /**
     * Clean up the tcc fence log a few days ago by default
     */
    public static final int DEFAULT_CLEAN_DAY = 1;

    /**
     * Clean up the tcc fence log a few hours ago by default
     */
    public static final int DEFAULT_CLEAN_HOUR = 1;

    /**
     * Clean up the tcc fence log a few minutes ago by default
     */
    public static final int DEFAULT_CLEAN_MINUTE = 15;

    /**
     * Default tcc fence log table name
     */
    public static final String DEFAULT_LOG_TABLE_NAME = "tcc_fence_log";

}
