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
package io.seata.spring.boot.autoconfigure.properties.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.LOCK_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = LOCK_PREFIX)
public class LockProperties {
    private int lockRetryInterval = 10;
    private int lockRetryTimes = 30;
    private boolean lockRetryPolicyBranchRollbackOnConflict = true;

    public int getLockRetryInterval() {
        return lockRetryInterval;
    }

    public LockProperties setLockRetryInterval(int lockRetryInterval) {
        this.lockRetryInterval = lockRetryInterval;
        return this;
    }

    public int getLockRetryTimes() {
        return lockRetryTimes;
    }

    public LockProperties setLockRetryTimes(int lockRetryTimes) {
        this.lockRetryTimes = lockRetryTimes;
        return this;
    }

    public boolean isLockRetryPolicyBranchRollbackOnConflict() {
        return lockRetryPolicyBranchRollbackOnConflict;
    }

    public LockProperties setLockRetryPolicyBranchRollbackOnConflict(boolean lockRetryPolicyBranchRollbackOnConflict) {
        this.lockRetryPolicyBranchRollbackOnConflict = lockRetryPolicyBranchRollbackOnConflict;
        return this;
    }
}
