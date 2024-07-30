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
package org.apache.seata.core.model;


import org.apache.seata.common.exception.ShouldNeverHappenException;

/**
 * Status of branch transaction.
 *
 */
public enum BranchStatus {

    /**
     * The Unknown.
     * description:Unknown branch status.
     */
    Unknown(0),

    /**
     * The Registered.
     * description:Registered to TC.
     */
    Registered(1),

    /**
     * The Phase one done.
     * description:Branch logic is successfully done at phase one.
     */
    PhaseOne_Done(2),

    /**
     * The Phase one failed.
     * description:Branch logic is failed at phase one.
     */
    PhaseOne_Failed(3),

    /**
     * The Phase one timeout.
     * description:Branch logic is NOT reported for a timeout.
     */
    PhaseOne_Timeout(4),

    /**
     * The Phase two committed.
     * description:Commit logic is successfully done at phase two.
     */
    PhaseTwo_Committed(5),

    /**
     * The Phase two commit failed retryable.
     * description:Commit logic is failed but retryable.
     */
    PhaseTwo_CommitFailed_Retryable(6),

    /**
     * The Phase two commit failed unretryable.
     * description:Commit logic is failed and NOT retryable.
     */
    PhaseTwo_CommitFailed_Unretryable(7),

    /**
     * The Phase two rollbacked.
     * description:Rollback logic is successfully done at phase two.
     */
    PhaseTwo_Rollbacked(8),

    /**
     * The Phase two rollback failed retryable.
     * description:Rollback logic is failed but retryable.
     */
    PhaseTwo_RollbackFailed_Retryable(9),

    /**
     * The Phase two rollback failed unretryable.
     * description:Rollback logic is failed but NOT retryable.
     */
    PhaseTwo_RollbackFailed_Unretryable(10),

    /**
     * The Phase two commit failed retryable because of XAException.XAER_NOTA.
     * description:Commit logic is failed because of XAException.XAER_NOTA but retryable.
     */
    PhaseTwo_CommitFailed_XAER_NOTA_Retryable(11),

    /**
     * The Phase two rollback failed retryable because of XAException.XAER_NOTA.
     * description:rollback logic is failed because of XAException.XAER_NOTA but retryable.
     */
    PhaseTwo_RollbackFailed_XAER_NOTA_Retryable(12),


    /**
     * The results of the Phase one are read-only.
     * Description: After the branch prepare in the Oracle database, only purely read-only query statements were executed.
     */
    PhaseOne_RDONLY(13);

    private int code;

    BranchStatus(int code) {
        this.code = code;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }


    /**
     * Get branch status.
     *
     * @param code the code
     * @return the branch status
     */
    public static BranchStatus get(byte code) {
        return get((int)code);
    }

    /**
     * Get branch status.
     *
     * @param code the code
     * @return the branch status
     */
    public static BranchStatus get(int code) {
        BranchStatus value = null;
        try {
            value = BranchStatus.values()[code];
        } catch (Exception e) {
            throw new ShouldNeverHappenException("Unknown BranchStatus[" + code + "]");
        }
        return value;
    }

}
