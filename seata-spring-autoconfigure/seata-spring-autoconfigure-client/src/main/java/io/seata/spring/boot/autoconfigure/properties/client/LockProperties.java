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
package io.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_INTERVAL;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_LOCK_RETRY_TIMES;
import static io.seata.spring.boot.autoconfigure.StarterConstants.LOCK_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = LOCK_PREFIX)
public class LockProperties {
    private int retryInterval = DEFAULT_CLIENT_LOCK_RETRY_INTERVAL;
    private int retryTimes = DEFAULT_CLIENT_LOCK_RETRY_TIMES;
    private boolean retryPolicyBranchRollbackOnConflict = DEFAULT_CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT;

    public int getRetryInterval() {
        return retryInterval;
    }

    public LockProperties setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public LockProperties setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
        return this;
    }

    public boolean isRetryPolicyBranchRollbackOnConflict() {
        return retryPolicyBranchRollbackOnConflict;
    }

    public LockProperties setRetryPolicyBranchRollbackOnConflict(boolean retryPolicyBranchRollbackOnConflict) {
        this.retryPolicyBranchRollbackOnConflict = retryPolicyBranchRollbackOnConflict;
        return this;
    }
}
