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
package io.seata.integration.thread;

import io.seata.core.context.RootContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

public class ContextTest {

    static ExecutorService executorService;

    @BeforeAll
    public static void before() {
        executorService = new ThreadPoolExecutor(1, 1, 1, TimeUnit.HOURS, new LinkedBlockingQueue<>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "contextText-" + System.currentTimeMillis());
            }
        });
    }

    @Test
    public void threadTest() throws InterruptedException {
        RootContext.bind("test-context");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Assertions.assertEquals(RootContext.getXID(), "test-context");
            }
        });
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Assertions.assertEquals(RootContext.getXID(), "test-context");
                return null;
            }
        });
    }

    @Test
    public void nonPropagateRunnable() throws InterruptedException {

        RootContext.bind("none-context");
        executorService.submit(new NonPropagateRunnable() {
            @Override
            public void run() {
                Assertions.assertEquals(RootContext.getXID(), "none-context");
            }
        });
        executorService.submit(new NonPropagateCallable<Object>() {
            @Override
            public Object call() throws Exception {
                Assertions.assertEquals(RootContext.getXID(), "none-context");
                return null;
            }
        });
    }
}
