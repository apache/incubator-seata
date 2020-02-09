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

import static io.seata.core.constants.DefaultValues.DEFAULT_TM_COMMIT_RETRY_COUNT;
import static io.seata.core.constants.DefaultValues.DEFAULT_TM_ROLLBACK_RETRY_COUNT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_TM_PREFIX;

/**
 * @author xingfudeshi@gmail.com
 */
@Component
@ConfigurationProperties(prefix = CLIENT_TM_PREFIX)
public class TmProperties {
    private int commitRetryCount = DEFAULT_TM_COMMIT_RETRY_COUNT;
    private int rollbackRetryCount = DEFAULT_TM_ROLLBACK_RETRY_COUNT;

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
}
