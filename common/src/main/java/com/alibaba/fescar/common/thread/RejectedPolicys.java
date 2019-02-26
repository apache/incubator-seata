package com.alibaba.fescar.common.thread;
/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * policys for RejectedExecutionHandler
 *
 * Created by guoyao on 2019/2/26.
 */
public final class RejectedPolicys {

    /**
     * when rejected happened ,add the new task and run the oldest task
     * @return
     */
    public static RejectedExecutionHandler runsOldestTaskPolicy() {
        return new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                if (executor.isShutdown()) {
                    return;
                }
                BlockingQueue<Runnable> workQueue=executor.getQueue();
                Runnable firstWork=workQueue.poll();
                workQueue.offer(r);
                if (firstWork != null) {
                    firstWork.run();
                }
            }
        };
    }
}
