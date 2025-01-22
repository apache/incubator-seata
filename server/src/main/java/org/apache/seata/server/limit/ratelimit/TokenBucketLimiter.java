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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.common.util.NumberUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * TokenBucketLimiter based on Bucket4j
 */
@LoadLevel(name = "token-bucket-limiter", scope = Scope.SINGLETON)
public class TokenBucketLimiter implements RateLimiter, Initialize {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBucketLimiter.class);

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
    private Integer bucketTokenInitialNum;

    /**
     * the Bucket
     */
    private Bucket bucket;

    private static final int DEFAULT_BUCKET_TOKEN_NUM_PER_SECOND = Integer.MAX_VALUE;
    private static final int DEFAULT_BUCKET_TOKEN_MAX_NUM = Integer.MAX_VALUE;
    private static final int DEFAULT_BUCKET_TOKEN_INITIAL_NUM = Integer.MAX_VALUE;

    public TokenBucketLimiter() {}

    public TokenBucketLimiter(boolean enable, Integer bucketTokenNumPerSecond,
                              Integer bucketTokenMaxNum, Integer bucketTokenInitialNum) {
        this.enable = enable;
        this.bucketTokenNumPerSecond = bucketTokenNumPerSecond;
        this.bucketTokenMaxNum = bucketTokenMaxNum;
        this.bucketTokenInitialNum = bucketTokenInitialNum;
        initBucket();
    }

    @Override
    public void init() {
        final Configuration config = ConfigurationFactory.getInstance();
        this.enable = config.getBoolean(ConfigurationKeys.RATE_LIMIT_ENABLE);
        this.bucketTokenNumPerSecond = NumberUtils.toInt(
                config.getConfig(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_NUM_PER_SECOND),
                DEFAULT_BUCKET_TOKEN_NUM_PER_SECOND
        );
        this.bucketTokenMaxNum = NumberUtils.toInt(
                config.getConfig(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_MAX_NUM),
                DEFAULT_BUCKET_TOKEN_MAX_NUM
        );
        this.bucketTokenInitialNum = NumberUtils.toInt(
                config.getConfig(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_INITIAL_NUM),
                DEFAULT_BUCKET_TOKEN_INITIAL_NUM
        );

        if (this.enable) {
            initBucket();
            LOGGER.info("TokenBucketLimiter init success, bucketTokenNumPerSecond: {}, tokenMaxNum: {}, tokenInitialNum: {}",
                    this.bucketTokenNumPerSecond, this.bucketTokenMaxNum, this.bucketTokenInitialNum);
        }
    }

    @Override
    public boolean canPass() {
        return bucket.tryConsume(1);
    }

    @Override
    public void reInit(RateLimiterHandlerConfig config) {
        this.enable = config.isEnable();
        this.bucketTokenNumPerSecond = config.getBucketTokenNumPerSecond();
        this.bucketTokenMaxNum = config.getBucketTokenMaxNum();
        this.bucketTokenInitialNum = config.getBucketTokenInitialNum();

        if (this.enable) {
            initBucket();
            LOGGER.info("TokenBucketLimiter reInit success, bucketTokenNumPerSecond: {}, tokenMaxNum: {}, tokenInitialNum: {}",
                    this.bucketTokenNumPerSecond, this.bucketTokenMaxNum, this.bucketTokenInitialNum);
            return;
        }
        LOGGER.info("TokenBucketLimiter reInit success, The limiter is disabled");
    }

    @Override
    public RateLimiterHandlerConfig obtainConfig() {
        RateLimiterHandlerConfig config = new RateLimiterHandlerConfig();
        config.setEnable(this.enable);
        config.setBucketTokenNumPerSecond(this.bucketTokenNumPerSecond);
        config.setBucketTokenMaxNum(this.bucketTokenMaxNum);
        config.setBucketTokenInitialNum(this.bucketTokenInitialNum);
        return config;
    }

    @Override
    public boolean isEnable() {
        return this.enable;
    }

    private void initBucket() {
        Bandwidth limit = Bandwidth.classic(this.bucketTokenMaxNum, Refill.greedy(this.bucketTokenNumPerSecond,
                Duration.ofSeconds(1)));
        Bucket bucket = Bucket.builder().addLimit(limit).build();
        if (this.bucketTokenInitialNum > 0) {
            bucket.addTokens(this.bucketTokenInitialNum);
        }
        this.bucket = bucket;
    }

}
