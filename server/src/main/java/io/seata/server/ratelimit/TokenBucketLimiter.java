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

import static io.seata.common.DefaultValues.DEFAULT_SERVER_RATELIMIT_DELAY;

/**
 * RateLimiter based on the token bucket algorithm.
 */
@LoadLevel(name = "token-bucket", scope = Scope.PROTOTYPE)
public class TokenBucketLimiter implements RateLimiter, Initialize {

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
    private int burst;

    /**
     * The last time to update tokens in microseconds.
     */
    private long lastUpdateTimeInMicros;

    /**
     * Check whether the request should be queued or discarded when there is no token.
     */
    private boolean delay;

    private Object lock = new Object();

    public TokenBucketLimiter() {
    }

    public TokenBucketLimiter(double requestsPerSecond) {
        this(requestsPerSecond, (int) requestsPerSecond);
    }

    public TokenBucketLimiter(double requestsPerSecond, int burst) {
        this(requestsPerSecond, burst, false);
    }

    public TokenBucketLimiter(double requestsPerSecond, boolean delay) {
        this(requestsPerSecond, (int)requestsPerSecond, delay);
    }

    public TokenBucketLimiter(double requestsPerSecond, int burst, boolean delay) {
        this.numOfToken = 1;
        this.microSecondsPerToken = TimeUnit.SECONDS.toMicros(1L) / requestsPerSecond;
        this.burst = burst;
        this.lastUpdateTimeInMicros = microTime();
        this.delay = delay;
    }

    @Override
    public void init() {
        double requestsPerSecond = Double.parseDouble(CONFIG.getConfig(ConfigurationKeys.REQUESTS_PER_SECOND));
        this.microSecondsPerToken = TimeUnit.SECONDS.toMicros(1L) / requestsPerSecond;
        this.burst = CONFIG.getInt(ConfigurationKeys.BURST, (int)microSecondsPerToken);
        this.delay = CONFIG.getBoolean(ConfigurationKeys.DELAY, DEFAULT_SERVER_RATELIMIT_DELAY);
        this.numOfToken = 1;
        this.lastUpdateTimeInMicros = microTime();
    }

    @Override
    public boolean canPass() {
        if (delay) {
            return acquire();
        }
        return tryAcquire();
    }

    public boolean acquire() {
        long waitTimeInMicros = tryGetTokenWithDelay();
        try {
            TimeUnit.MICROSECONDS.sleep(waitTimeInMicros);
        } catch (InterruptedException e) {
            e.printStackTrace();
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