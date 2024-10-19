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

package seata.e2e.helper;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A synchronous fixed number of task executor
 *
 * @author jingliu_xiong@foxmail.com
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
    private int count = 0;

    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(Runtime.getRuntime().
            availableProcessors(), r -> {
        final Thread thread = new Thread(r, "TimesTask");
        thread.setDaemon(true);
        return thread;
    });


    public void start() {
        TimeCountHelper timeCountHelper = new TimeCountHelper();
        timeCountHelper.startTimeCount();
        final CountDownLatch countDownLatch = new CountDownLatch(this.times);
        executorService.scheduleAtFixedRate(() -> {
            try {
                if (countDownLatch.getCount() > 0) {
                    final Object result = sender.call();
                    LOGGER.info("response: {}", result);
                    count++;
                } else {
                    List<Runnable> runnables = executorService.shutdownNow();
                    if (runnables.size() != 0) {
                        LOGGER.error("there are some requests not work, the num is ", runnables.size());
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
        long time = timeCountHelper.stopTimeCount();
        LOGGER.info("task cost time: {} s", time / 1000);
        LOGGER.info("task completed, total {}", count);
        LOGGER.info("task defeated, total {}", times - count);
        count = 0;
    }

}