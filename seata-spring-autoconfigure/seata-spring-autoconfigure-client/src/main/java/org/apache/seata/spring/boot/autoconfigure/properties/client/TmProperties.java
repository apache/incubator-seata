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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.common.DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_COMMIT_RETRY_COUNT;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_DEGRADE_CHECK;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_DEGRADE_CHECK_PERIOD;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_ROLLBACK_RETRY_COUNT;
import static org.apache.seata.common.DefaultValues.TM_INTERCEPTOR_ORDER;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_TM_PREFIX;


@Component
@ConfigurationProperties(prefix = CLIENT_TM_PREFIX)
public class TmProperties {
    private int commitRetryCount = DEFAULT_TM_COMMIT_RETRY_COUNT;
    private int rollbackRetryCount = DEFAULT_TM_ROLLBACK_RETRY_COUNT;
    private int defaultGlobalTransactionTimeout = DEFAULT_GLOBAL_TRANSACTION_TIMEOUT;
    private boolean degradeCheck = DEFAULT_TM_DEGRADE_CHECK;
    private int degradeCheckAllowTimes = DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES;
    private int degradeCheckPeriod = DEFAULT_TM_DEGRADE_CHECK_PERIOD;
    private int interceptorOrder = TM_INTERCEPTOR_ORDER;

    public int getCommitRetryCount() {
        return commitRetryCount;
    }

    public TmProperties setCommitRetryCount(int commitRetryCount) {
        this.commitRetryCount = commitRetryCount;
        return this;
    }

    public int getRollbackRetryCount() {
        return rollbackRetryCount;
    }

    public TmProperties setRollbackRetryCount(int rollbackRetryCount) {
        this.rollbackRetryCount = rollbackRetryCount;
        return this;
    }

    public int getDefaultGlobalTransactionTimeout() {
        return defaultGlobalTransactionTimeout;
    }

    public TmProperties setDefaultGlobalTransactionTimeout(int defaultGlobalTransactionTimeout) {
        this.defaultGlobalTransactionTimeout = defaultGlobalTransactionTimeout;
        return this;
    }

    public boolean isDegradeCheck() {
        return degradeCheck;
    }

    public TmProperties setDegradeCheck(boolean degradeCheck) {
        this.degradeCheck = degradeCheck;
        return this;
    }

    public int getDegradeCheckPeriod() {
        return degradeCheckPeriod;
    }

    public TmProperties setDegradeCheckPeriod(int degradeCheckPeriod) {
        this.degradeCheckPeriod = degradeCheckPeriod;
        return this;
    }

    public int getDegradeCheckAllowTimes() {
        return degradeCheckAllowTimes;
    }

    public void setDegradeCheckAllowTimes(int degradeCheckAllowTimes) {
        this.degradeCheckAllowTimes = degradeCheckAllowTimes;
    }

    public int getInterceptorOrder() {
        return interceptorOrder;
    }

    public TmProperties setInterceptorOrder(int interceptorOrder) {
        this.interceptorOrder = interceptorOrder;
        return this;
    }
}
