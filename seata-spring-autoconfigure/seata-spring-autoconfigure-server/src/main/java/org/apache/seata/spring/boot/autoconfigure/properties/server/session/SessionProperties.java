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
package org.apache.seata.spring.boot.autoconfigure.properties.server.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SESSION_PREFIX;

/**
 * session properties
 *
 * @since 2022-01-07 17:39
 */
@Component
@ConfigurationProperties(prefix = SESSION_PREFIX)
public class SessionProperties {

    /**
     * branch async remove queue size
     */
    private Integer branchAsyncQueueSize;

    /**
     * enable to asynchronous remove branchSession
     */
    private Boolean enableBranchAsyncRemove = false;

    public Integer getBranchAsyncQueueSize() {
        return branchAsyncQueueSize;
    }

    public SessionProperties setBranchAsyncQueueSize(Integer branchAsyncQueueSize) {
        this.branchAsyncQueueSize = branchAsyncQueueSize;
        return this;
    }

    public Boolean getEnableBranchAsync() {
        return enableBranchAsyncRemove;
    }

    public SessionProperties setEnableBranchAsync(Boolean enableBranchAsyncRemove) {
        this.enableBranchAsyncRemove = enableBranchAsyncRemove;
        return this;
    }
}
