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
package io.seata.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;

public class LockAndCallback {
    private final Lock lock;
    private final Condition notFinished;
    private final AsyncCallback callback;
    private final static long DEFAULT_TIMEOUT = 60000;
    private String result;

    public LockAndCallback() {
        lock = new ReentrantLock();
        notFinished = lock.newCondition();
        callback = new AsyncCallback() {
            @Override
            public void onFinished(ProcessContext context, StateMachineInstance stateMachineInstance) {
                result = "onFinished";
                try {
                    lock.lock();
                    notFinished.signal();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public void onError(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {
                result = "onError";
                try {
                    lock.lock();
                    notFinished.signal();
                } finally {
                    lock.unlock();
                }
            }
        };
    }
    public void waitingForFinish(StateMachineInstance inst) {
        waitingForFinish(inst, DEFAULT_TIMEOUT);
    }

    public void waitingForFinish(StateMachineInstance inst, long timeout) {
        if (ExecutionStatus.RU.equals(inst.getStatus())) {
            long start = System.nanoTime();
            try {
                lock.lock();
                boolean finished = notFinished.await(timeout, TimeUnit.MILLISECONDS);
                if (finished) {
                    System.out.printf("finish wait ====== XID: %s, status: %s, compensationStatus: %s, cost: %d ms, result: %s\r\n",
                            inst.getId(), inst.getStatus(), inst.getCompensationStatus(), (System.nanoTime() - start) / 1000_000, result);
                } else {
                    System.out.printf("timeout wait ====== XID: %s, status: %s, compensationStatus: %s, cost: %d ms, result: %s\r\n",
                            inst.getId(), inst.getStatus(), inst.getCompensationStatus(), (System.nanoTime() - start) / 1000_000, result);
                }
            } catch (Exception e) {
                System.out.printf("error wait ====== XID: %s, status: %s, compensationStatus: %s, cost: %d ms, result: %s, error: %s\r\n",
                        inst.getId(), inst.getStatus(), inst.getCompensationStatus(), (System.nanoTime() - start) / 1000_000, result, e.getMessage());
                throw new RuntimeException("waitingForFinish failed", e);
            } finally {
                lock.unlock();
            }
        } else {
            System.out.printf("do not wait ====== XID: %s, status: %s, compensationStatus: %s, result: %s\r\n",
                    inst.getId(), inst.getStatus(), inst.getCompensationStatus(), result);
        }
    }

    public AsyncCallback getCallback() {
        return callback;
    }
}
