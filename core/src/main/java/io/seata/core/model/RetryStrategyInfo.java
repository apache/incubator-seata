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
package io.seata.core.model;

/**
 * Retry strategy info
 *
 * @author wang.liang
 */
public class RetryStrategyInfo implements RetryStrategy {

    private Long retryExpire;

    private Integer maxRetryCount;

    //the fields for the first type of interval
    private Integer[] retryIntervalPlan;

    //the fields for the second type of interval
    private Integer retryInterval;
    private Boolean isIncremental;
    private Integer maxRetryInterval;

    public RetryStrategyInfo() {
    }

    public RetryStrategyInfo(Integer maxRetryCount, Integer[] retryIntervalPlan, Long retryExpire) {
        this.maxRetryCount = maxRetryCount;
        this.retryIntervalPlan = retryIntervalPlan;
        this.retryExpire = retryExpire;
    }

    public RetryStrategyInfo(Integer maxRetryCount, Integer retryInterval, Boolean isIncremental, Integer maxRetryInterval, Long retryExpire) {
        this.maxRetryCount = maxRetryCount;
        this.retryInterval = retryInterval;
        this.isIncremental = isIncremental;
        this.maxRetryInterval = maxRetryInterval;
        this.retryExpire = retryExpire;
    }

    public RetryStrategyInfo(String retryStrategy) {
        this.valueOf(retryStrategy);
    }

    /**
     * is overtime
     *
     * @param globalTransactionBeginTime the global transaction begin time
     * @return the boolean
     */
    @Override
    public boolean isExpired(long globalTransactionBeginTime) {
        if (this.retryExpire != null && this.retryExpire > 0) {
            return globalTransactionBeginTime + this.retryExpire > System.currentTimeMillis();
        } else {
            return false;
        }
    }

    /**
     * is reached max retry count
     *
     * @param branchRetryCount
     * @return the boolean
     */
    @Override
    public boolean isReachedMaxRetryCount(int branchRetryCount) {
        if (this.maxRetryCount != null && this.maxRetryCount > 0) {
            return branchRetryCount >= this.maxRetryCount;
        } else {
            return false;
        }
    }

    /**
     * next retry interval
     *
     * @param currentRetryCount the count of retries so far
     * @return the retry interval. No retry strategy if null.
     */
    @Override
    public Long nextRetryInterval(int currentRetryCount) {

        return 0L;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public void valueOf(String retryStrategy) {

    }

    /**
     * is empty instance
     *
     * @return
     */
    public boolean isEmpty() {
        return (maxRetryCount == null || maxRetryCount <= 0 || maxRetryCount == Integer.MAX_VALUE)
                && (retryIntervalPlan == null || retryIntervalPlan.length == 0)
                && (retryInterval == null || retryInterval <= 0 || retryInterval == Integer.MAX_VALUE)
                && isIncremental == null
                && (maxRetryInterval == null || maxRetryInterval <= 0 || maxRetryInterval == Integer.MAX_VALUE)
                && (retryExpire == null || retryExpire <= 0 || retryExpire == Long.MAX_VALUE);
    }


    // Gets and Sets

    public Long getRetryExpire() {
        return retryExpire;
    }

    public void setRetryExpire(Long retryExpire) {
        this.retryExpire = retryExpire;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Integer[] getRetryIntervalPlan() {
        return retryIntervalPlan;
    }

    public void setRetryIntervalPlan(Integer[] retryIntervalPlan) {
        this.retryIntervalPlan = retryIntervalPlan;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Boolean getIncremental() {
        return isIncremental;
    }

    public void setIncremental(Boolean incremental) {
        isIncremental = incremental;
    }

    public Integer getMaxRetryInterval() {
        return maxRetryInterval;
    }

    public void setMaxRetryInterval(Integer maxRetryInterval) {
        this.maxRetryInterval = maxRetryInterval;
    }
}
