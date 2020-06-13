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

import io.seata.common.util.StringUtils;

/**
 * Retry strategy info
 *
 * @author wang.liang
 */
public class RetryStrategyInfo implements RetryStrategy {

    public static final String STABLE_MODE = "S";
    public static final String INCREMENTAL_MODE = "I";
    public static final String PLAN_MODE = "P";

    // P=Plan, S=Stable, I=Incremental
    private String mode;

    private Integer retryExpire; //unit is Seconds
    private Integer maxRetryCount;

    //the fields for the mode S and I
    private Integer retryInterval; //unit is Seconds

    //the fields for the mode I
    private Integer maxRetryInterval; //unit is Seconds

    //the fields for the mode P
    private int[] retryIntervalPlan; //unit is Seconds


    public RetryStrategyInfo() {
    }

    // mode S
    public RetryStrategyInfo(Integer retryExpire, Integer maxRetryCount, int retryInterval) {
        this.mode = STABLE_MODE;
        this.retryExpire = retryExpire;
        this.maxRetryCount = maxRetryCount;
        this.retryInterval = retryInterval;
    }

    // mode I
    public RetryStrategyInfo(Integer retryExpire, Integer maxRetryCount, int retryInterval, Integer maxRetryInterval) {
        this.mode = INCREMENTAL_MODE;
        this.retryExpire = retryExpire;
        this.maxRetryCount = maxRetryCount;
        this.retryInterval = retryInterval;
        this.maxRetryInterval = maxRetryInterval;
    }

    // mode P
    public RetryStrategyInfo(Integer retryExpire, Integer maxRetryCount, int[] retryIntervalPlan) {
        this.mode = PLAN_MODE;
        this.retryExpire = retryExpire;
        this.maxRetryCount = maxRetryCount;
        this.retryIntervalPlan = retryIntervalPlan;
    }

    public RetryStrategyInfo(String retryStrategy) {
        this.valueOf(retryStrategy);
    }

    /**
     * is expired
     *
     * @param globalTransactionBeginTime the global transaction begin time
     * @return the boolean
     */
    @Override
    public boolean isExpired(long globalTransactionBeginTime) {
        if (this.retryExpire != null && this.retryExpire > 0) {
            return System.currentTimeMillis() > globalTransactionBeginTime + this.retryExpire * 1000;
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
     * @param branchRetryCount the count of the branch retries so far
     * @return the retry interval（unit is milliseconds）. No retry strategy if null.
     */
    @Override
    public Long nextRetryInterval(int branchRetryCount) {
        if (mode == null) {
            return null;
        }

        if (branchRetryCount <= 0) {
            throw new IllegalArgumentException("branchRetryCount must be greater than 0");
        }

        int interval;
        if (STABLE_MODE.equalsIgnoreCase(mode) && retryInterval != null && retryInterval > 0) {
            interval = retryInterval;
        } else if (INCREMENTAL_MODE.equalsIgnoreCase(mode) && retryInterval != null && retryInterval > 0) {
            interval = retryInterval * branchRetryCount;
            if (maxRetryInterval != null && maxRetryInterval > 0 && interval > maxRetryInterval) {
                interval = maxRetryInterval;
            }
        } else if (PLAN_MODE.equalsIgnoreCase(mode) && retryIntervalPlan != null && retryIntervalPlan.length > 0) {
            if (branchRetryCount >= retryIntervalPlan.length) {
                interval = retryIntervalPlan[retryIntervalPlan.length - 1];
            } else {
                interval = retryIntervalPlan[branchRetryCount - 1];
            }
        } else {
            return null;
        }

        return (long) (1000 * interval);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // mode
        if (mode == null || mode.length() > 1 || !(STABLE_MODE + INCREMENTAL_MODE + PLAN_MODE).contains(mode.toUpperCase())) {
            if (retryIntervalPlan != null && retryIntervalPlan.length > 0) {
                mode = PLAN_MODE;
            } else if (retryInterval != null && retryInterval > 0) {
                if (maxRetryInterval != null && maxRetryInterval > 0) {
                    mode = INCREMENTAL_MODE;
                } else {
                    mode = STABLE_MODE;
                }
            } else {
                //unknown mode
                return "";
            }
        }
        sb.append(mode.toUpperCase()).append(",");

        // expire
        sb.append(retryExpire == null || retryExpire <= 0 ? 0 : retryExpire).append(",");

        // max retry count
        sb.append(maxRetryCount == null || maxRetryCount <= 0 ? 0 : maxRetryCount).append("|");

        if (STABLE_MODE.equalsIgnoreCase(mode) || INCREMENTAL_MODE.equalsIgnoreCase(mode)) {
            // S or I: retry interval
            sb.append(retryInterval);
            if (INCREMENTAL_MODE.equalsIgnoreCase(mode) && maxRetryInterval != null && maxRetryInterval > 0) {
                // I: max retry count
                sb.append(",").append(maxRetryInterval);
            }
        } else {
            // P: retry interval plan
            for (int i = 0, l = retryIntervalPlan.length; i < l; ++i) {
                sb.append(retryIntervalPlan[i]);
                if (i < l - 1) {
                    sb.append(",");
                }
            }
        }

        return sb.toString();
    }

    public void valueOf(String retryStrategy) {
        if (StringUtils.isBlank(retryStrategy)) {
            return;
        }

        //temporary variable
        String mode;
        Integer retryExpire; //unit is Seconds
        Integer maxRetryCount;
        Integer retryInterval = null; //unit is Seconds
        Integer maxRetryInterval = null; //unit is Seconds
        int[] retryIntervalPlan = null; //unit is Seconds

        try {
            String[] arr = retryStrategy.split("\\|");
            String[] arr1 = arr[0].split(",");
            String[] arr2 = arr[1].split(",");

            // mode
            mode = arr1[0].toUpperCase();
            if (mode == null || mode.isEmpty() || mode.length() > 1 || !(STABLE_MODE + INCREMENTAL_MODE + PLAN_MODE).contains(mode.toUpperCase())) {
                throw new Exception("Illegal mode: " + mode);
            }

            // retry expire
            retryExpire = Integer.valueOf(arr1[1]);
            if (retryExpire <= 0) {
                retryExpire = null;
            }

            // max retry count
            maxRetryCount = Integer.valueOf(arr1[2]);
            if (maxRetryCount <= 0) {
                maxRetryCount = null;
            }


            if (STABLE_MODE.equalsIgnoreCase(mode) || INCREMENTAL_MODE.equalsIgnoreCase(mode)) {
                // retry interval
                retryInterval = Integer.valueOf(arr2[0]);
                if (retryInterval <= 0) {
                    throw new Exception("Illegal retryInterval: " + retryInterval);
                }
                if (INCREMENTAL_MODE.equalsIgnoreCase(mode) && arr2.length >= 2) {
                    // max retry interval
                    maxRetryInterval = Integer.valueOf(arr2[1]);
                    if (maxRetryInterval <= 0) {
                        maxRetryInterval = null;
                    }
                }
            } else {
                // retry interval plan
                int interval;
                retryIntervalPlan = new int[arr2.length];
                for (int i = 0; i < arr2.length; ++i) {
                    interval = Integer.valueOf(arr2[i]);
                    if (interval <= 0) {
                        throw new Exception("Illegal retryIntervalPlan[" + i + "]: " + interval);
                    }
                    retryIntervalPlan[i] = interval;
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal string of the retry strategy: " + retryStrategy, e);
        }

        this.mode = mode.toUpperCase();
        this.retryExpire = retryExpire;
        this.maxRetryCount = maxRetryCount;
        this.retryInterval = retryInterval;
        this.maxRetryInterval = maxRetryInterval;
        this.retryIntervalPlan = retryIntervalPlan;
    }

    /**
     * is empty instance
     *
     * @return the boolean
     */
    public boolean isEmpty() {
        return (mode == null || mode.length() == 0)
                && (retryExpire == null || retryExpire <= 0)
                && (maxRetryCount == null || maxRetryCount <= 0)
                && (retryInterval == null || retryInterval <= 0)
                && (maxRetryInterval == null || maxRetryInterval <= 0)
                && (retryIntervalPlan == null || retryIntervalPlan.length == 0);
    }


    // Gets and Sets

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getRetryExpire() {
        return retryExpire;
    }

    public void setRetryExpire(Integer retryExpire) {
        this.retryExpire = retryExpire;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Integer getMaxRetryInterval() {
        return maxRetryInterval;
    }

    public void setMaxRetryInterval(Integer maxRetryInterval) {
        this.maxRetryInterval = maxRetryInterval;
    }

    public int[] getRetryIntervalPlan() {
        return retryIntervalPlan;
    }

    public void setRetryIntervalPlan(int[] retryIntervalPlan) {
        this.retryIntervalPlan = retryIntervalPlan;
    }
}
