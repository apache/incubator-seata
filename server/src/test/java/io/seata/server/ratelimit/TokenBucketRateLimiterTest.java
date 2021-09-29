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

import io.seata.common.thread.NamedThreadFactory;
import io.seata.server.ServerApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class TokenBucketRateLimiterTest {

    /**
     * Logger for TokenBucketRateLimiterTest
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBucketRateLimiterTest.class);

    public static void main(String[] args) throws IOException {
        // Init the CONFIG in the TokenBucketRateLimiter
        SpringApplication.run(ServerApplication.class, args);
        TokenBucketRateLimiterTest test = new TokenBucketRateLimiterTest();
        // test the performance
//        test.testPerformanceOfTokenBucketLimiter();
        // test the performance with warmup and default value of burst
//        test.testPerformanceOfTokenBucketLimiterWithWarmup();
        // test the performance with delay and default value of burst
//        test.testPerformanceOfTokenBucketLimiterWithDelay();
        test.testPerformanceOfGUAVATokenBucketLimiterWithDelay();
    }

    public void testPerformanceOfTokenBucketLimiter() {
        RateLimiter rateLimiter = new TokenBucketLimiter(1000);
        com.google.common.util.concurrent.RateLimiter rateLimiter1 = com.google.common.util.concurrent.RateLimiter.create(1000);
        int threads = 50;
        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("test1-", false));
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                int count = 1000;
                int pass = 0;
                int reject = 0;
                StopWatch w = new StopWatch();
                w.start();
                while (count > 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    boolean result = rateLimiter.canPass();
//                    boolean result = rateLimiter1.tryAcquire();
                    if (result) {
                        pass++;
                    } else {
                        reject++;
                    }
                    count--;
                }
                w.stop();
                LOGGER.info("total time:{}ms, pass:{}, reject:{}", w.getLastTaskTimeMillis(), pass, reject);
            });
        }
    }

    public void testPerformanceOfTokenBucketLimiterWithWarmup() {
        RateLimiter rateLimiter = new TokenBucketLimiter(1000);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int threads = 50;
        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("test2-", false));
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                int count = 1000;
                int pass = 0;
                int reject = 0;
                StopWatch w = new StopWatch();
                w.start();
                while (count > 0) {
                    boolean result = rateLimiter.canPass();
                    if (result) {
                        pass++;
                    } else {
                        reject++;
                    }
                    count--;
                }
                w.stop();
                LOGGER.info("total time:{}ms, pass:{}, reject:{}", w.getLastTaskTimeMillis(), pass, reject);
            });
        }
    }

    public void testPerformanceOfTokenBucketLimiterWithDelay() {
        RateLimiter rateLimiter = new TokenBucketLimiter(1000,true);
        int threads = 50;
        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("test3-", false));
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                int count = 100;
                int pass = 0;
                int reject = 0;
                StopWatch w = new StopWatch();
                w.start();
                while (count > 0) {
                    boolean result = rateLimiter.canPass();
                    if (result) {
                        pass++;
                    }
                    count--;
                }
                w.stop();
                LOGGER.info("total time:{}ms, pass:{}, reject:{}", w.getLastTaskTimeMillis(), pass, reject);
            });
        }
    }

    public void testPerformanceOfGUAVATokenBucketLimiterWithDelay() {
        com.google.common.util.concurrent.RateLimiter rateLimiter1 = com.google.common.util.concurrent.RateLimiter.create(1000);
        int threads = 50;
        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("test3-", false));
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                int count = 100;
                int pass = 0;
                int reject = 0;
                StopWatch w = new StopWatch();
                w.start();
                while (count > 0) {
                    double result1 = rateLimiter1.acquire();
                    if (true) {
                        pass++;
                    }
                    count--;
                }
                w.stop();
                LOGGER.info("total time:{}ms, pass:{}, reject:{}", w.getLastTaskTimeMillis(), pass, reject);
            });
        }
    }
}