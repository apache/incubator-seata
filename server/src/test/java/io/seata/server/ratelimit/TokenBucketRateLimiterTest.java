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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.thread.NamedThreadFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

@SpringBootTest
public class TokenBucketRateLimiterTest {

    /**
     * Logger for TokenBucketRateLimiterTest
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBucketRateLimiterTest.class);

    @Test
    public void testPerformanceOfTokenBucketLimiter() throws InterruptedException {
        RateLimiter rateLimiter = new TokenBucketLimiter(1000);
        int threads = 50;
        final int count = 1000;
        final CountDownLatch cnt = new CountDownLatch(count * threads);

        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("test1", false));
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                int pass = 0;
                int reject = 0;
                StopWatch w = new StopWatch();
                w.start();
                for (int u = 0; u < count; u++) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    boolean result = rateLimiter.canPass();
                    if (result) {
                        pass++;
                    } else {
                        reject++;
                    }
                    cnt.countDown();
                }
                w.stop();
                LOGGER.info("total time:{}ms, pass:{}, reject:{}", w.getLastTaskTimeMillis(), pass, reject);
            });
        }
        cnt.await();
    }

    @Test
    public void testPerformanceOfTokenBucketLimiterWithWarmup() throws InterruptedException {
        RateLimiter rateLimiter = new TokenBucketLimiter(1000);
        // warm up
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int threads = 50;
        final int count = 1000;
        final CountDownLatch cnt = new CountDownLatch(count * threads);

        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("test2", false));
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                int pass = 0;
                int reject = 0;
                StopWatch w = new StopWatch();
                w.start();
                for (int u = 0; u < count; u++) {
                    boolean result = rateLimiter.canPass();
                    if (result) {
                        pass++;
                    } else {
                        reject++;
                    }
                    cnt.countDown();
                }
                w.stop();
                LOGGER.info("total time:{}ms, pass:{}, reject:{}", w.getLastTaskTimeMillis(), pass, reject);
            });
        }
        cnt.await();
    }

    @Test
    public void testPerformanceOfTokenBucketLimiterWithDelay() throws InterruptedException {
        RateLimiter rateLimiter = new TokenBucketLimiter(1000, true, 1000);
        int threads = 50;
        final int count = 1000;
        final CountDownLatch cnt = new CountDownLatch(count * threads);

        final ThreadPoolExecutor service1 = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory("test3", false));
        for (int i = 0; i < threads; i++) {
            service1.execute(() -> {
                int pass = 0;
                int reject = 0;
                StopWatch w = new StopWatch();
                w.start();
                for (int u = 0; u < count; u++) {
                    boolean result = rateLimiter.canPass();
                    if (result) {
                        pass++;
                    }
                    cnt.countDown();
                }
                w.stop();
                LOGGER.info("total time:{}ms, pass:{}, reject:{}", w.getLastTaskTimeMillis(), pass, reject);
            });
        }
        cnt.await();
    }
}