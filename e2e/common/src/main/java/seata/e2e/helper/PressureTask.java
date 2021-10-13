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

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A synchronous pressure task executor.
 *
 * @author jingliu_xiong@foxmail.com
 */
public class PressureTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressureTask.class);


    public PressureTask(Callable<?> sender, int clientTotal, int threadTotal) {
        this.sender = sender;
        this.clientTotal = clientTotal;
        this.threadTotal = threadTotal;
        this.executorService =  new ThreadPoolExecutor(0, threadTotal,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                r -> {
                    final Thread thread = new Thread(r, "PressureTask");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    private Callable<?> sender;
    private int clientTotal;
    private int threadTotal ;
    private final ExecutorService executorService;
    private int count = 0;

    /**
     * The returned result of the sender is successful by default
     * @param isRetry when this parameter is set to true, any test failure in this pressure task
     * will throw an RuntimeException.
     */
    public void start(boolean isRetry){

        TimeCountHelper timeCountHelper = new TimeCountHelper();
        timeCountHelper.startTimeCount();
        final Semaphore semaphore = new Semaphore(threadTotal);
        final CountDownLatch countDownLatch = new CountDownLatch(clientTotal);
        for (int i = 0; i < clientTotal ; i++) {
            executorService.execute(() -> {
                try {
                    semaphore.acquire();
                    final Object result = sender.call();
                    LOGGER.info("response: {}", result);
                    count++;
                } catch (Exception e) {
                    LOGGER.error("exception", e);
                } finally {
                    semaphore.release();
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long time = timeCountHelper.stopTimeCount();
        LOGGER.info("pressure task cost time: {} mills", time);
        LOGGER.info("pressure task completed, total {}", count);
        LOGGER.info("pressure task defeated, total {}", clientTotal - count);
        if (isRetry && (count != clientTotal)) {
            count = 0;
            throw new RuntimeException("Pressure task defeated!");
        }
        count = 0;
    }

    /**
     * The returned result of the sender is judged by judger.
     * @param isRetry
     * @param judger
     */
    public void start(boolean isRetry, Function<Object, Boolean> judger){

        TimeCountHelper timeCountHelper = new TimeCountHelper();
        timeCountHelper.startTimeCount();
        final Semaphore semaphore = new Semaphore(threadTotal);
        final CountDownLatch countDownLatch = new CountDownLatch(clientTotal);
        for (int i = 0; i < clientTotal ; i++) {
            executorService.execute(() -> {
                Object result = null;
                try {
                    semaphore.acquire();
                    result = sender.call();
                    LOGGER.info("response: {}", result);
                    Boolean b = false;
                    b = (Boolean) judger.apply(result);
                    if (b == true) {
                        count++;
                    }
                } catch (Exception e) {
                    LOGGER.error("exception", e);
                } finally {
                    semaphore.release();
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long time = timeCountHelper.stopTimeCount();
        LOGGER.info("pressure task cost time: {} mills", time);
        LOGGER.info("pressure task completed, total {}", count);
        LOGGER.info("pressure task defeated, total {}", clientTotal - count);
        if (isRetry && (count != clientTotal)) {
            count = 0;
            throw new RuntimeException("pressure task defeated !");
        }
        count = 0;
    }

}