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

package com.demo.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

/**
 * Perform tasks at fixed number of times. 同步
 */
public class TimesTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimesTask.class);

    public TimesTask(Callable<?> sender, int times, int interval) {
        this.sender = sender;
        this.times = times;
        this.interval = interval;
    }

    private Callable<?> sender;
    private int times;
    private int interval;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10, r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    public void start() {
        final CountDownLatch countDownLatch = new CountDownLatch(this.times);
        executorService.scheduleAtFixedRate(() -> {
            try {
                if (countDownLatch.getCount() > 0) {
                    final Object result = sender.call();
                    LOGGER.info("response: {}", result);
                } else {
                    List<Runnable> runnables = executorService.shutdownNow();
                    if (runnables.size() != 0) {
                        LOGGER.error("There are some requests not work, the num is ", runnables.size());
                    }
                }
            } catch (final Exception e) {
                LOGGER.error("failed to send request", e);
            } finally {
                countDownLatch.countDown();
            }
        }, 0, this.interval, TimeUnit.MILLISECONDS);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}