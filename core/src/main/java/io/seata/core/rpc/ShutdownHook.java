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
package io.seata.core.rpc;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ensure the shutdownHook is singleton
 *
 * @author 563868273@qq.com
 */
public class ShutdownHook extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);

    private static final ShutdownHook SHUTDOWN_HOOK = new ShutdownHook("ShutdownHook");

    private final PriorityQueue<DisposablePriorityWrapper> disposables = new PriorityQueue<>();

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    /**
     * default 10. Lower values have higher priority
     */
    private static final int DEFAULT_PRIORITY = 10;

    static {
        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);
    }

    private ShutdownHook(String name) {
        super(name);
    }

    public static ShutdownHook getInstance() {
        return SHUTDOWN_HOOK;
    }

    public void addDisposable(Disposable disposable) {
        addDisposable(disposable, DEFAULT_PRIORITY);
    }

    public void addDisposable(Disposable disposable, int priority) {
        disposables.add(new DisposablePriorityWrapper(disposable, priority));
    }

    @Override
    public void run() {
        destroyAll();
    }

    public void destroyAll() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        if (disposables.isEmpty()) {
            return;
        }

        LOGGER.debug("destoryAll starting");

        while (!disposables.isEmpty()) {
            Disposable disposable = disposables.poll();
            disposable.destroy();
        }

        LOGGER.debug("destoryAll finish");
    }

    /**
     * for spring context
     */
    public static void removeRuntimeShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(SHUTDOWN_HOOK);
    }

    private static class DisposablePriorityWrapper implements Comparable<DisposablePriorityWrapper>, Disposable {

        private final Disposable disposable;

        private final int priority;

        public DisposablePriorityWrapper(Disposable disposable, int priority) {
            this.disposable = disposable;
            this.priority = priority;
        }

        @Override
        public int compareTo(DisposablePriorityWrapper challenger) {
            return priority - challenger.priority;
        }

        @Override
        public void destroy() {
            disposable.destroy();
        }
    }
}

