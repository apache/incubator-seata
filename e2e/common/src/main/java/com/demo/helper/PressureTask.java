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

import java.util.concurrent.*;

/**
 * @author xjl
 * @Description: 压力测试 同步
 */
public class PressureTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressureTask.class);

    public PressureTask(Callable<?> sender, int clientTotal, int threadTotal) {
        this.sender = sender;
        this.clientTotal = clientTotal;
        this.threadTotal = threadTotal;
    }

    private Callable<?> sender;
    private int clientTotal;
    private int threadTotal;
    private final ExecutorService executorService =  Executors.newCachedThreadPool();
    private int count = 0;


    public void start(){

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
        executorService.shutdown();
        LOGGER.info("pressure task completed, total {}", count);
    }

}