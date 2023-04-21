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
 * The redis key constants
 *
 * @author wangzhongxiang
 */
public class RedisKeyConstants {

    /**
     * The constant redis key of global transaction name xid
     */
    public static final String REDIS_KEY_GLOBAL_XID = "xid";

    /**
     * The constant redis key of global transaction name transactionId
     */
    public static final String REDIS_KEY_GLOBAL_TRANSACTION_ID = "transactionId";

    /**
     * The constant redis key of global transaction name status
     */
    public static final String REDIS_KEY_GLOBAL_STATUS = "status";

    /**
     * The constant redis key of global transaction name applicationId
     */
    public static final String REDIS_KEY_GLOBAL_APPLICATION_ID = "applicationId";

    /**
     * The constant redis key of global transaction name transactionServiceGroup
     */
    public static final String REDIS_KEY_GLOBAL_TRANSACTION_SERVICE_GROUP = "transactionServiceGroup";

    /**
     * The constant redis key of global transaction name transactionName
     */
    public static final String REDIS_KEY_GLOBAL_TRANSACTION_NAME = "transactionName";

    /**
     * The constant redis key of global transaction name timeout
     */
    public static final String REDIS_KEY_GLOBAL_TIMEOUT = "timeout";

    /**
     * The constant redis key of global transaction name beginTime
     */
    public static final String REDIS_KEY_GLOBAL_BEGIN_TIME = "beginTime";

    /**
     * The constant redis key of global transaction name applicationData
     */
    public static final String REDIS_KEY_GLOBAL_APPLICATION_DATA = "applicationData";

    /**
     * The constant redis key of global transaction name gmtCreate
     */
    public static final String REDIS_KEY_GLOBAL_GMT_CREATE = "gmtCreate";

    /**
     * The constant redis key of global transaction name gmtModified
     */
    public static final String REDIS_KEY_GLOBAL_GMT_MODIFIED = "gmtModified";





    /**
     * The constant redis key of branch transaction name branchId
     */
    public static final String REDIS_KEY_BRANCH_BRANCH_ID = "branchId";

    /**
     * The constant redis key of branch transaction name xid
     */
    public static final String REDIS_KEY_BRANCH_XID = "xid";

    /**
     * The constant redis key of branch transaction name transactionId
     */
    public static final String REDIS_KEY_BRANCH_TRANSACTION_ID = "transactionId";

    /**
     * The constant redis key of branch transaction name resourceGroupId
     */
    public static final String REDIS_KEY_BRANCH_RESOURCE_GROUP_ID = "resourceGroupId";

    /**
     * The constant redis key of branch transaction name resourceId
     */
    public static final String REDIS_KEY_BRANCH_RESOURCE_ID = "resourceId";

    /**REDIS_
     * The constant redis key of branch transaction name branchType
     */
    public static final String REDIS_KEY_BRANCH_BRANCH_TYPE = "branchType";

    /**
     * The constant redis key of branch transaction name status
     */
    public static final String REDIS_KEY_BRANCH_STATUS = "status";

    /**
     * The constant redis key of branch transaction name beginTime
     */
    public static final String REDIS_KEY_BRANCH_BEGIN_TIME = "beginTime";

    /**
     * The constant redis key of branch transaction name applicationData
     */
    public static final String REDIS_KEY_BRANCH_APPLICATION_DATA = "applicationData";

    /**
     * The constant redis key of branch transaction name clientId
     */
    public static final String REDIS_KEY_BRANCH_CLIENT_ID = "clientId";

    /**
     * The constant redis key of branch transaction name gmtCreate
     */
    public static final String REDIS_KEY_BRANCH_GMT_CREATE = "gmtCreate";

    /**
     * The constant redis key of branch transaction name gmtModified
     */
    public static final String REDIS_KEY_BRANCH_GMT_MODIFIED = "gmtModified";

    /**
     * The globalLock key
     */
    public static final String DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX = "SEATA_GLOBAL_LOCK";

    /**
     * The globalLock keys
     */
    public static final String DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX = "SEATA_ROW_LOCK_";

    /**
     * The split
     */
    public static final String SPLIT = "^^^";

    /**
     * The constant DEFAULT_LOG_QUERY_LIMIT.
     */
    public static final int DEFAULT_LOG_QUERY_LIMIT = 100;

}
