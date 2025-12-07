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
package org.apache.seata.tm.api.transaction;

import org.apache.seata.common.LockStrategyMode;
import org.apache.seata.common.util.CollectionUtils;

import java.io.Serializable;
import java.util.Set;

/**
 * Transaction configuration and metadata container for global transactions.
 *
 * <p>Encapsulates all configuration parameters that control how a global transaction
 * should be executed within Seata framework.</p>
 *
 * <p><b>Configuration Categories:</b></p>
 * <ul>
 *   <li><b>Timing</b>: Timeout settings and duration limits</li>
 *   <li><b>Identification</b>: Transaction naming for monitoring</li>
 *   <li><b>Propagation</b>: How to handle existing transaction contexts</li>
 *   <li><b>Rollback Rules</b>: Exception-based rollback decision logic</li>
 *   <li><b>Lock Management</b>: Global lock retry and strategy configuration</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * TransactionInfo info = TransactionInfo.newBuilder()
 *     .setTimeOut(30000)
 *     .setName("order-processing")
 *     .setPropagation(Propagation.REQUIRED)
 *     .addRollbackRule(BusinessException.class)
 *     .setLockRetryTimes(5)
 *     .build();
 * }</pre>
 *
 * @author Seata Team
 * @see org.apache.seata.tm.api.TransactionalExecutor#getTransactionInfo()
 * @see Propagation
 * @since 1.0.0
 */
public final class TransactionInfo implements Serializable {

    /**
     * Transaction timeout in milliseconds.
     * Specifies maximum duration for transaction execution.
     * Default: 60000ms, Recommended range: 1000-300000ms
     */
    private int timeOut;

    /**
     * Transaction name for identification and monitoring.
     * Used for log correlation, debugging, and performance analysis.
     * Best practices: descriptive names like "order-create", keep under 64 chars
     */
    private String name;

    /**
     * Set of rollback rules determining exception-based rollback behavior.
     * Contains RollbackRule and NoRollbackRule instances that define
     * which exceptions should or should not trigger transaction rollback.
     */
    private Set<RollbackRule> rollbackRules;

    /**
     * Transaction propagation behavior.
     * Determines how this transaction interacts with existing transaction contexts.
     * Default: REQUIRED (use existing or create new)
     */
    private Propagation propagation;

    /**
     * Interval between global lock acquisition retry attempts in milliseconds.
     * Default: 100ms, Range: 10-5000ms
     */
    private int lockRetryInterval;

    /**
     * Maximum number of global lock acquisition retry attempts.
     * Default: 30, Range: 1-100
     * Total wait time = lockRetryTimes × lockRetryInterval
     */
    private int lockRetryTimes;

    /**
     * Global lock strategy mode.
     * OPTIMISTIC: acquire locks during commit (better performance, late conflicts)
     * PESSIMISTIC: acquire locks during execution (early detection, potential deadlocks)
     */
    private LockStrategyMode lockStrategyMode;

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<RollbackRule> getRollbackRules() {
        return rollbackRules;
    }

    public void setRollbackRules(Set<RollbackRule> rollbackRules) {
        this.rollbackRules = rollbackRules;
    }

    public boolean rollbackOn(Throwable ex) {

        RollbackRule winner = null;
        int deepest = Integer.MAX_VALUE;

        if (CollectionUtils.isNotEmpty(rollbackRules)) {
            winner = NoRollbackRule.DEFAULT_NO_ROLLBACK_RULE;
            for (RollbackRule rule : this.rollbackRules) {
                int depth = rule.getDepth(ex);
                if (depth >= 0 && depth < deepest) {
                    deepest = depth;
                    winner = rule;
                }
            }
        }

        return !(winner instanceof NoRollbackRule);
    }

    public Propagation getPropagation() {
        if (this.propagation != null) {
            return this.propagation;
        }
        // default propagation
        return Propagation.REQUIRED;
    }

    public void setPropagation(Propagation propagation) {
        this.propagation = propagation;
    }

    public int getLockRetryInterval() {
        return lockRetryInterval;
    }

    public void setLockRetryInterval(int lockRetryInterval) {
        this.lockRetryInterval = lockRetryInterval;
    }

    public int getLockRetryTimes() {
        return lockRetryTimes;
    }

    public void setLockRetryTimes(int lockRetryTimes) {
        this.lockRetryTimes = lockRetryTimes;
    }

    public LockStrategyMode getLockStrategyMode() {
        return lockStrategyMode;
    }

    public void setLockStrategyMode(LockStrategyMode lockStrategyMode) {
        this.lockStrategyMode = lockStrategyMode;
    }
}
