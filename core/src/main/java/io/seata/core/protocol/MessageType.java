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
 */
public interface MessageType {

    /**
     * The constant TYPE_GLOBAL_BEGIN.
     */
    short TYPE_GLOBAL_BEGIN = 1;
    /**
     * The constant TYPE_GLOBAL_BEGIN_RESULT.
     */
    short TYPE_GLOBAL_BEGIN_RESULT = 2;
    /**
     * The constant TYPE_GLOBAL_COMMIT.
     */
    short TYPE_GLOBAL_COMMIT = 7;
    /**
     * The constant TYPE_GLOBAL_COMMIT_RESULT.
     */
    short TYPE_GLOBAL_COMMIT_RESULT = 8;
    /**
     * The constant TYPE_GLOBAL_ROLLBACK.
     */
    short TYPE_GLOBAL_ROLLBACK = 9;
    /**
     * The constant TYPE_GLOBAL_ROLLBACK_RESULT.
     */
    short TYPE_GLOBAL_ROLLBACK_RESULT = 10;
    /**
     * The constant TYPE_GLOBAL_STATUS.
     */
    short TYPE_GLOBAL_STATUS = 15;
    /**
     * The constant TYPE_GLOBAL_STATUS_RESULT.
     */
    short TYPE_GLOBAL_STATUS_RESULT = 16;
    /**
     * The constant TYPE_GLOBAL_REPORT.
     */
    short TYPE_GLOBAL_REPORT = 17;
    /**
     * The constant TYPE_GLOBAL_REPORT_RESULT.
     */
    short TYPE_GLOBAL_REPORT_RESULT = 18;
    /**
     * The constant TYPE_GLOBAL_LOCK_QUERY.
     */
    short TYPE_GLOBAL_LOCK_QUERY = 21;
    /**
     * The constant TYPE_GLOBAL_LOCK_QUERY_RESULT.
     */
    short TYPE_GLOBAL_LOCK_QUERY_RESULT = 22;

    /**
     * The constant TYPE_BRANCH_COMMIT.
     */
    short TYPE_BRANCH_COMMIT = 3;
    /**
     * The constant TYPE_BRANCH_COMMIT_RESULT.
     */
    short TYPE_BRANCH_COMMIT_RESULT = 4;
    /**
     * The constant TYPE_BRANCH_ROLLBACK.
     */
    short TYPE_BRANCH_ROLLBACK = 5;
    /**
     * The constant TYPE_BRANCH_ROLLBACK_RESULT.
     */
    short TYPE_BRANCH_ROLLBACK_RESULT = 6;
    /**
     * The constant TYPE_BRANCH_REGISTER.
     */
    short TYPE_BRANCH_REGISTER = 11;
    /**
     * The constant TYPE_BRANCH_REGISTER_RESULT.
     */
    short TYPE_BRANCH_REGISTER_RESULT = 12;
    /**
     * The constant TYPE_BRANCH_STATUS_REPORT.
     */
    short TYPE_BRANCH_STATUS_REPORT = 13;
    /**
     * The constant TYPE_BRANCH_STATUS_REPORT_RESULT.
     */
    short TYPE_BRANCH_STATUS_REPORT_RESULT = 14;

    /**
     * The constant TYPE_SEATA_MERGE.
     */
    short TYPE_SEATA_MERGE = 59;
    /**
     * The constant TYPE_SEATA_MERGE_RESULT.
     */
    short TYPE_SEATA_MERGE_RESULT = 60;

    /**
     * The constant TYPE_REG_CLT.
     */
    short TYPE_REG_CLT = 101;
    /**
     * The constant TYPE_REG_CLT_RESULT.
     */
    short TYPE_REG_CLT_RESULT = 102;
    /**
     * The constant TYPE_REG_RM.
     */
    short TYPE_REG_RM = 103;
    /**
     * The constant TYPE_REG_RM_RESULT.
     */
    short TYPE_REG_RM_RESULT = 104;
    /**
     * The constant TYPE_RM_DELETE_UNDOLOG.
     */
    short TYPE_RM_DELETE_UNDOLOG = 111;

    /**
     * the constant TYPE_HEARTBEAT_MSG
     */
    short TYPE_HEARTBEAT_MSG = 120;
}
