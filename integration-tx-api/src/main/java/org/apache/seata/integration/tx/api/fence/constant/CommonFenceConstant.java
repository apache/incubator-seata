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
package org.apache.seata.integration.tx.api.fence.constant;

/**
 * Common Fence Constant
 *
 */
public class CommonFenceConstant {

    private CommonFenceConstant() {
        throw new IllegalStateException("Utility class");
    }

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
}
