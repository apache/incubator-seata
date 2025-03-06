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
package org.apache.seata.server.limit.ratelimit;

/**
 * RateLimiterHandlerConfig
 */
public class RateLimiterHandlerConfig {
    /**
     * whether enable server rate limit
     */
    private boolean enable;

    /**
     * limit token number of bucket per second
     */
    private int bucketTokenNumPerSecond;

    /**
     * limit token max number of bucket
     */
    private int bucketTokenMaxNum;

    /**
     * limit token initial number of bucket
     */
    private int bucketTokenInitialNum;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getBucketTokenNumPerSecond() {
        return bucketTokenNumPerSecond;
    }

    public void setBucketTokenNumPerSecond(int bucketTokenNumPerSecond) {
        this.bucketTokenNumPerSecond = bucketTokenNumPerSecond;
    }

    public int getBucketTokenMaxNum() {
        return bucketTokenMaxNum;
    }

    public void setBucketTokenMaxNum(int bucketTokenMaxNum) {
        this.bucketTokenMaxNum = bucketTokenMaxNum;
    }

    public int getBucketTokenInitialNum() {
        return bucketTokenInitialNum;
    }

    public void setBucketTokenInitialNum(int bucketTokenInitialNum) {
        this.bucketTokenInitialNum = bucketTokenInitialNum;
    }
}
