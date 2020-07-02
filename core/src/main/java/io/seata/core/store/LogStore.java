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
package io.seata.core.store;


import io.seata.core.model.GlobalStatus;

import java.util.List;

import static io.seata.core.constants.DefaultValues.FIRST_PAGE_INDEX;

/**
 * the transaction log store
 *
 * @author zhangsen
 */
public interface LogStore<G extends GlobalTransactionDO, B extends BranchTransactionDO> {

    /**
     * Get global transaction do global transaction do.
     *
     * @param xid the xid
     * @return the global transaction do
     */
    G getGlobalTransactionDO(String xid);

    /**
     * Get global transaction do global transaction do.
     *
     * @param transactionId the transaction id
     * @return the global transaction do
     */
    G getGlobalTransactionDO(long transactionId);

    /**
     * Find global transaction do list.
     *
     * @param condition the condition
     * @return the list
     */
    List<G> findGlobalTransactionDO(GlobalTransactionCondition condition);

    /**
     * Find global transaction do list.
     *
     * @param statuses the statuses
     * @return the list
     */
    default List<G> findGlobalTransactionDO(GlobalStatus... statuses) {
        return this.findGlobalTransactionDO(new GlobalTransactionCondition(statuses));
    }

    /**
     * Find global transaction do list.
     *
     * @param statuses the statuses
     * @param limit    the limit
     * @return the list
     */
    default List<G> findGlobalTransactionDO(GlobalStatus[] statuses, int limit) {
        return this.findGlobalTransactionDO(new GlobalTransactionCondition(statuses, FIRST_PAGE_INDEX, limit));
    }

    /**
     * Count global transaction do.
     *
     * @param condition the condition
     * @return the count
     */
    int countGlobalTransactionDO(GlobalTransactionCondition condition);

    /**
     * Count global transaction do.
     *
     * @param statuses the statuses
     * @return the list
     */
    default int countGlobalTransactionDO(GlobalStatus... statuses) {
        return this.countGlobalTransactionDO(new GlobalTransactionCondition(statuses));
    }

    /**
     * Insert global transaction do boolean.
     *
     * @param globalTransactionDO the global transaction do
     * @return the boolean
     */
    boolean insertGlobalTransactionDO(G globalTransactionDO);

    /**
     * Update global transaction do boolean.
     *
     * @param globalTransactionDO the global transaction do
     * @return the boolean
     */
    boolean updateGlobalTransactionDO(G globalTransactionDO);

    /**
     * Delete global transaction do boolean.
     *
     * @param globalTransactionDO the global transaction do
     * @return the boolean
     */
    boolean deleteGlobalTransactionDO(G globalTransactionDO);

    /**
     * Find branch transaction do list.
     *
     * @param xid the xid
     * @return the BranchTransactionDO list
     */
    List<B> findBranchTransactionDO(String xid);

    /**
     * Find branch transaction do list.
     *
     * @param xids the xid list
     * @return the BranchTransactionDO list
     */
    List<B> findBranchTransactionDO(List<String> xids);

    /**
     * Insert branch transaction do boolean.
     *
     * @param branchTransactionDO the branch transaction do
     * @return the boolean
     */
    boolean insertBranchTransactionDO(B branchTransactionDO);

    /**
     * Update branch transaction do boolean.
     *
     * @param branchTransactionDO the branch transaction do
     * @return the boolean
     */
    boolean updateBranchTransactionDO(B branchTransactionDO);

    /**
     * Delete branch transaction do boolean.
     *
     * @param branchTransactionDO the branch transaction do
     * @return the boolean
     */
    boolean deleteBranchTransactionDO(B branchTransactionDO);

    /**
     * Gets current max session id.
     *
     * @param high the high
     * @param low  the low
     * @return the current max session id
     */
    long getCurrentMaxSessionId(long high, long low);

}
