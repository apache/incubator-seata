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
package io.seata.server.ratelimit;

import java.util.concurrent.TimeUnit;

import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_DELAY_TIMEOUT;
import static io.seata.common.DefaultValues.DEFAULT_SERVER_RATELIMIT_DELAY;

/**
 * RateLimiter based on the token bucket algorithm.
 */
@LoadLevel(name = "token-bucket", scope = Scope.SINGLETON)
public class TokenBucketLimiter implements RateLimiter, Initialize {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBucketLimiter.class);

    /**
     * The constant CONFIG.
     */
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The number of tokens.
     */
    private double numOfToken;

    /**
     * The time to produce a token in microseconds.
     */
    private double microSecondsPerToken;

    /**
     * The maximum number of tokens.
     */
    private double burst;

    /**
     * The last time to update tokens in microseconds.
     */
    private long lastUpdateTimeInMicros;

    /**
     * Check whether the request should be queued or discarded when there is no token.
     */
    private boolean delay;

    /**
     * Timeout in milliseconds.
     */
    private long timeout;

    private Object lock = new Object();

    public TokenBucketLimiter() {
    }

    public TokenBucketLimiter(double requestsPerSecond) {
        this(requestsPerSecond, requestsPerSecond);
    }

    public TokenBucketLimiter(double requestsPerSecond, double burst) {
        this(requestsPerSecond, burst, DEFAULT_SERVER_RATELIMIT_DELAY, DEFAULT_DELAY_TIMEOUT);
    }

    public TokenBucketLimiter(double requestsPerSecond, boolean delay, long timeout) {
        this(requestsPerSecond, requestsPerSecond, delay, timeout);
    }

    public TokenBucketLimiter(double requestsPerSecond, double burst, boolean delay, long timeout) {
        this.numOfToken = 1;
        this.microSecondsPerToken = TimeUnit.SECONDS.toMicros(1L) / requestsPerSecond;
        this.burst = burst;
        this.lastUpdateTimeInMicros = microTime();
        this.delay = delay;
        this.timeout = timeout;
    }

    @Override
    public void init() {
        this.numOfToken = 1;
        String requestsPerSecondConfig = CONFIG.getConfig(ConfigurationKeys.REQUESTS_PER_SECOND);
        if (requestsPerSecondConfig == null) {
            throw new IllegalArgumentException("ratelimiter requestsPerSecond is null");
        }
        double requestsPerSecond;
        try {
            requestsPerSecond = Double.parseDouble(requestsPerSecondConfig);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("could not convert '" + requestsPerSecondConfig + "' to requestsPerSecond");
        }
        this.microSecondsPerToken = TimeUnit.SECONDS.toMicros(1L) / requestsPerSecond;
        String burstConfig = CONFIG.getConfig(ConfigurationKeys.BURST, String.valueOf(requestsPerSecond));
        try {
            this.burst = Double.parseDouble(burstConfig);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("could not convert '" + burstConfig + "' to burst");
        }
        this.lastUpdateTimeInMicros = microTime();
        this.delay = CONFIG.getBoolean(ConfigurationKeys.DELAY, DEFAULT_SERVER_RATELIMIT_DELAY);
        if (delay) {
            this.timeout = CONFIG.getLong(ConfigurationKeys.DELAY_TIMEOUT, DEFAULT_DELAY_TIMEOUT);
        }
    }

    @Override
    public boolean canPass() {
        if (delay) {
            return acquire();
        }
        return tryAcquire();
    }

    public boolean acquire() {
        return acquire(timeout);
    }

    public boolean acquire(long timeout) {
        long waitTimeInMicros = tryGetTokenWithDelay();
        if (TimeUnit.MICROSECONDS.toMillis(waitTimeInMicros) > timeout) {
            return false;
        }
        try {
            TimeUnit.MICROSECONDS.sleep(waitTimeInMicros);
        } catch (InterruptedException e) {
            LOGGER.warn("Sleep to wait for token error:{}", e.getMessage(), e);
        }
        return true;
    }

    public boolean tryAcquire() {
        if (!tryGetToken()) {
            return false;
        }
        return true;
    }

    private long tryGetTokenWithDelay() {
        synchronized (lock) {
            long nowInMicros = microTime();
            numOfToken = Math.min((nowInMicros - lastUpdateTimeInMicros) / microSecondsPerToken + numOfToken, burst);
            lastUpdateTimeInMicros = nowInMicros;
            numOfToken -= 1;
            if (numOfToken >= 0) {
                return 0;
            }
            long waitTimeInMicros = (long)((-numOfToken) * microSecondsPerToken);
            return waitTimeInMicros;
        }
    }

    private boolean tryGetToken() {
        synchronized (lock) {
            long nowInMicros = microTime();
            numOfToken = Math.min((nowInMicros - lastUpdateTimeInMicros) / microSecondsPerToken + numOfToken, burst);
            lastUpdateTimeInMicros = nowInMicros;
            if (numOfToken >= 1) {
                numOfToken -= 1;
                return true;
            }
            return false;
        }
    }

    private long microTime() {
        return TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
    }
}