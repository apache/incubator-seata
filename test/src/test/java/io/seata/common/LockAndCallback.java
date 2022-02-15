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
package io.seata.common;

import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * @author wang.liang
 */
public class LockAndCallback {
    private final Object lock;
    private final AsyncCallback callback;

    private String result;

    public LockAndCallback() {
        lock = new Object();
        callback = new AsyncCallback() {
            @Override
            public void onFinished(ProcessContext context, StateMachineInstance stateMachineInstance) {
                result = "onFinished";
                synchronized (lock) {
                    lock.notifyAll();
                }
            }

            @Override
            public void onError(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {
                result = "onError";
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        };
    }

    public void waittingForFinish(StateMachineInstance inst) {
        synchronized (lock) {
            if (ExecutionStatus.RU.equals(inst.getStatus())) {
                long start = System.nanoTime();
                try {
                    lock.wait(30000);
                    System.out.printf("finish wait ====== XID: %s, status: %s, compensationStatus: %s, cost: %d ms, result: %s\r\n",
                            inst.getId(), inst.getStatus(), inst.getCompensationStatus(), (System.nanoTime() - start) / 1000_000, result);
                } catch (Exception e) {
                    System.out.printf("error wait ====== XID: %s, status: %s, compensationStatus: %s, cost: %d ms, result: %s, error: %s\r\n",
                            inst.getId(), inst.getStatus(), inst.getCompensationStatus(), (System.nanoTime() - start) / 1000_000, result, e.getMessage());
                    throw new RuntimeException("waittingForFinish failed", e);
                }
            } else {
                System.out.printf("do not wait ====== XID: %s, status: %s, compensationStatus: %s, result: %s\r\n",
                        inst.getId(), inst.getStatus(), inst.getCompensationStatus(), result);
            }
        }
    }

    public AsyncCallback getCallback() {
        return callback;
    }
}
