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
package org.apache.seata.spring.boot.autoconfigure.properties.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_PREFIX;


@Component
@ConfigurationProperties(prefix = SERVER_PREFIX)
public class ServerProperties {
    private long maxCommitRetryTimeout = -1L;
    private long maxRollbackRetryTimeout = -1L;
    private Boolean rollbackRetryTimeoutUnlockEnable = false;
    private Boolean enableCheckAuth = true;
    private Boolean enableParallelRequestHandle = true;
    private Boolean enableParallelHandleBranch = false;
    private Integer retryDeadThreshold = 130000;
    private Integer servicePort;
    private Integer xaerNotaRetryTimeout = 60000;

    private Boolean applicationDataLimitCheck = false;
    private Integer applicationDataLimit = 64000;

    public long getMaxCommitRetryTimeout() {
        return maxCommitRetryTimeout;
    }

    public ServerProperties setMaxCommitRetryTimeout(long maxCommitRetryTimeout) {
        this.maxCommitRetryTimeout = maxCommitRetryTimeout;
        return this;
    }

    public long getMaxRollbackRetryTimeout() {
        return maxRollbackRetryTimeout;
    }

    public ServerProperties setMaxRollbackRetryTimeout(long maxRollbackRetryTimeout) {
        this.maxRollbackRetryTimeout = maxRollbackRetryTimeout;
        return this;
    }

    public Boolean getRollbackRetryTimeoutUnlockEnable() {
        return rollbackRetryTimeoutUnlockEnable;
    }

    public ServerProperties setRollbackRetryTimeoutUnlockEnable(Boolean rollbackRetryTimeoutUnlockEnable) {
        this.rollbackRetryTimeoutUnlockEnable = rollbackRetryTimeoutUnlockEnable;
        return this;
    }

    public Boolean getEnableCheckAuth() {
        return enableCheckAuth;
    }

    public ServerProperties setEnableCheckAuth(Boolean enableCheckAuth) {
        this.enableCheckAuth = enableCheckAuth;
        return this;
    }

    public Integer getRetryDeadThreshold() {
        return retryDeadThreshold;
    }

    public ServerProperties setRetryDeadThreshold(Integer retryDeadThreshold) {
        this.retryDeadThreshold = retryDeadThreshold;
        return this;
    }

    public Integer getServicePort() {
        return servicePort;
    }

    public ServerProperties setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
        return this;
    }

    public Integer getXaerNotaRetryTimeout() {
        return xaerNotaRetryTimeout;
    }

    public void setXaerNotaRetryTimeout(Integer xaerNotaRetryTimeout) {
        this.xaerNotaRetryTimeout = xaerNotaRetryTimeout;
    }

    public Boolean getEnableParallelRequestHandle() {
        return enableParallelRequestHandle;
    }

    public void setEnableParallelRequestHandle(Boolean enableParallelRequestHandle) {
        this.enableParallelRequestHandle = enableParallelRequestHandle;
    }

    public Boolean getEnableParallelHandleBranch() {
        return enableParallelHandleBranch;
    }

    public void setEnableParallelHandleBranch(Boolean enableParallelHandleBranch) {
        this.enableParallelHandleBranch = enableParallelHandleBranch;
    }

    public Boolean getApplicationDataLimitCheck() {
        return applicationDataLimitCheck;
    }

    public void setApplicationDataLimitCheck(Boolean applicationDataLimitCheck) {
        this.applicationDataLimitCheck = applicationDataLimitCheck;
    }

    public Integer getApplicationDataLimit() {
        return applicationDataLimit;
    }

    public void setApplicationDataLimit(Integer applicationDataLimit) {
        this.applicationDataLimit = applicationDataLimit;
    }
}
