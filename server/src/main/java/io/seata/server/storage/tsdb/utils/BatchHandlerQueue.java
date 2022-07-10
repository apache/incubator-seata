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
package io.seata.server.storage.tsdb.utils;

import io.seata.server.storage.tsdb.Handler;
import io.seata.server.storage.tsdb.api.Event;
import io.seata.server.storage.tsdb.api.EventTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BatchHandlerQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchHandlerQueue.class);
    private final int queueNumber;
    private final int queueSize;
    private final int batchSize;

    private final Handler handler;

    private final ArrayBlockingQueue<Event>[] queues;

    public static BatchHandlerQueue getInstance() {
        return BatchHandlerQueueHolder.INSTANCE;
    }

    protected BatchHandlerQueue(int queueSize, int batchSize) {
        this.queueNumber = EventTopic.values().length;
        this.queueSize = queueSize;
        this.batchSize = batchSize;
        this.handler = new Handler();
        queues = new ArrayBlockingQueue[this.queueNumber];
        for (int i = 0; i < queueNumber; i++) {
            queues[i] = new ArrayBlockingQueue<>(this.queueSize);
            Thread t =
                    new TsdbQueueConsumer(
                            BatchHandlerQueue.class.getSimpleName()
                                    + "-"
                                    + i,
                            queues[i]);
            t.setDaemon(true);
            t.start();
        }
    }

    private void handle(ArrayList<Event> events){
        handler.handle(events);
    }

    public boolean offer(Event event) {
        if (event.topic != null) {
            return queues[event.topic.ordinal()].offer(event);
        } else {
            throw new IllegalArgumentException("event topic cannot be null or empty");
        }
    }

    public void put(Event event) throws InterruptedException {
        if (event.topic != null) {
            queues[event.topic.ordinal()].put(event);
        } else {
            throw new IllegalArgumentException("event topic cannot be null or empty");
        }
    }

    class TsdbQueueConsumer extends Thread {

        ArrayBlockingQueue<Event> queue;

        public TsdbQueueConsumer(String name, ArrayBlockingQueue<Event> queue) {
            super(name);
            this.queue = queue;
        }

        public void run() {
            final long maxWaitMillis = 500;
            final ArrayList<Event> list = new ArrayList<>();
            long startMillis = System.currentTimeMillis();
            long restMillis = maxWaitMillis;
            while (true) {
                try {
                    Event obj;
                    if (list.isEmpty()) {
                        obj = queue.take();
                    } else {
                        obj = queue.poll(restMillis, TimeUnit.MILLISECONDS);
                    }
                    if (obj != null) {
                        list.add(obj);
                        queue.drainTo(list, batchSize - list.size());
                        if (list.size() < batchSize) {
                            long waitMillis = System.currentTimeMillis() - startMillis;
                            if (waitMillis < maxWaitMillis) {
                                restMillis = maxWaitMillis - waitMillis;
                                continue;
                            }
                        }
                    }
                    handle(list);
                    list.clear();
                    startMillis = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable t) {
                    LOGGER.error("TsdbTaskQueue consumer error", t);
                }
            }
        }
    }

    private static class BatchHandlerQueueHolder {

        //TODO use config
        private static final BatchHandlerQueue INSTANCE = new BatchHandlerQueue(2000,20);

        private BatchHandlerQueueHolder() {}
    }
}
