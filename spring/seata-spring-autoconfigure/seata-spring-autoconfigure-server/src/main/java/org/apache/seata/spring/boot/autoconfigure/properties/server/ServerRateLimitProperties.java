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

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RATELIMIT_PREFIX;

@Component
@ConfigurationProperties(prefix = SERVER_RATELIMIT_PREFIX)
public class ServerRateLimitProperties {
    /**
     * whether enable server rate limit
     */
    private boolean enable;

    /**
     * limit token number of bucket per second
     */
    private Integer bucketTokenNumPerSecond;

    /**
     * limit token max number of bucket
     */
    private Integer bucketTokenMaxNum;

    /**
     * limit token initial number of bucket
     */
    private Integer bucketTokenInitialTime;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public Integer getBucketTokenNumPerSecond() {
        return bucketTokenNumPerSecond;
    }

    public void setBucketTokenNumPerSecond(Integer bucketTokenNumPerSecond) {
        this.bucketTokenNumPerSecond = bucketTokenNumPerSecond;
    }

    public Integer getBucketTokenMaxNum() {
        return bucketTokenMaxNum;
    }

    public void setBucketTokenMaxNum(Integer bucketTokenMaxNum) {
        this.bucketTokenMaxNum = bucketTokenMaxNum;
    }

    public Integer getBucketTokenInitialTime() {
        return bucketTokenInitialTime;
    }

    public void setBucketTokenInitialTime(Integer bucketTokenInitialTime) {
        this.bucketTokenInitialTime = bucketTokenInitialTime;
    }
}
