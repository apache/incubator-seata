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
package io.seata.core.protocol;

/**
 * The type Message codec type.
 *
 * @author zhangsen
 * @data 2019 /5/6
 */
public class MessageType {

    /**
     * The constant TYPE_GLOBAL_BEGIN.
     */
    public static final short TYPE_GLOBAL_BEGIN = 1;
    /**
     * The constant TYPE_GLOBAL_BEGIN_RESULT.
     */
    public static final short TYPE_GLOBAL_BEGIN_RESULT = 2;
    /**
     * The constant TYPE_GLOBAL_COMMIT.
     */
    public static final short TYPE_GLOBAL_COMMIT = 7;
    /**
     * The constant TYPE_GLOBAL_COMMIT_RESULT.
     */
    public static final short TYPE_GLOBAL_COMMIT_RESULT = 8;
    /**
     * The constant TYPE_GLOBAL_ROLLBACK.
     */
    public static final short TYPE_GLOBAL_ROLLBACK = 9;
    /**
     * The constant TYPE_GLOBAL_ROLLBACK_RESULT.
     */
    public static final short TYPE_GLOBAL_ROLLBACK_RESULT = 10;
    /**
     * The constant TYPE_GLOBAL_STATUS.
     */
    public static final short TYPE_GLOBAL_STATUS = 15;
    /**
     * The constant TYPE_GLOBAL_STATUS_RESULT.
     */
    public static final short TYPE_GLOBAL_STATUS_RESULT = 16;
    /**
     * The constant TYPE_GLOBAL_LOCK_QUERY.
     */
    public static final short TYPE_GLOBAL_LOCK_QUERY = 21;
    /**
     * The constant TYPE_GLOBAL_LOCK_QUERY_RESULT.
     */
    public static final short TYPE_GLOBAL_LOCK_QUERY_RESULT = 22;

    /**
     * The constant TYPE_BRANCH_COMMIT.
     */
    public static final short TYPE_BRANCH_COMMIT = 3;
    /**
     * The constant TYPE_BRANCH_COMMIT_RESULT.
     */
    public static final short TYPE_BRANCH_COMMIT_RESULT = 4;
    /**
     * The constant TYPE_BRANCH_ROLLBACK.
     */
    public static final short TYPE_BRANCH_ROLLBACK = 5;
    /**
     * The constant TYPE_BRANCH_ROLLBACK_RESULT.
     */
    public static final short TYPE_BRANCH_ROLLBACK_RESULT = 6;
    /**
     * The constant TYPE_BRANCH_REGISTER.
     */
    public static final short TYPE_BRANCH_REGISTER = 11;
    /**
     * The constant TYPE_BRANCH_REGISTER_RESULT.
     */
    public static final short TYPE_BRANCH_REGISTER_RESULT = 12;
    /**
     * The constant TYPE_BRANCH_STATUS_REPORT.
     */
    public static final short TYPE_BRANCH_STATUS_REPORT = 13;
    /**
     * The constant TYPE_BRANCH_STATUS_REPORT_RESULT.
     */
    public static final short TYPE_BRANCH_STATUS_REPORT_RESULT = 14;

    /**
     * The constant TYPE_SEATA_MERGE.
     */
    public static final short TYPE_SEATA_MERGE = 59;
    /**
     * The constant TYPE_SEATA_MERGE_RESULT.
     */
    public static final short TYPE_SEATA_MERGE_RESULT = 60;

    /**
     * The constant TYPE_REG_CLT.
     */
    public static final short TYPE_REG_CLT = 101;
    /**
     * The constant TYPE_REG_CLT_RESULT.
     */
    public static final short TYPE_REG_CLT_RESULT = 102;
    /**
     * The constant TYPE_REG_RM.
     */
    public static final short TYPE_REG_RM = 103;
    /**
     * The constant TYPE_REG_RM_RESULT.
     */
    public static final short TYPE_REG_RM_RESULT = 104;
}
