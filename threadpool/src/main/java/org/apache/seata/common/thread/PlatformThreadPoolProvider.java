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
package org.apache.seata.common.thread;

import org.apache.seata.common.loader.LoadLevel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Default SPI implementation that preserves the current Seata thread pool behavior.
 */
@LoadLevel(name = "platform", order = ThreadPoolProviderOrders.DEFAULT_PROVIDER_ORDER)
public class PlatformThreadPoolProvider implements ThreadPoolProvider {

    @Override
    public ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            boolean daemon,
            RejectedExecutionHandler rejectedHandler) {
        return new PlatformThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                new NamedThreadFactory(threadPrefix, maximumPoolSize, daemon),
                rejectedHandler);
    }

    @Override
    public ScheduledThreadPoolExecutor newScheduledThreadPoolExecutor(
            String threadPrefix, int corePoolSize, boolean daemon, RejectedExecutionHandler rejectedHandler) {
        return new ScheduledThreadPoolExecutor(
                corePoolSize, new NamedThreadFactory(threadPrefix, corePoolSize, daemon), rejectedHandler);
    }
}
