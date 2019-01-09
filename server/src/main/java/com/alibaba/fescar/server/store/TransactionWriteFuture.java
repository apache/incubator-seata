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

package com.alibaba.fescar.server.store;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fescar.server.store.TransactionStoreManager.LogOperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Transaction write future.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018 /12/10 14:49
 * @FileName: TransactionWriteFuture
 * @Description:
 */
public class TransactionWriteFuture {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionWriteFuture.class);
    private static final long DEFAULT_WRITE_TIMEOUT = 5 * 1000;
    private final long writeId;
    private long timeoutMills;
    private long start = System.currentTimeMillis();
    private volatile Object result;
    private final TransactionWriteStore writeStore;
    private final CountDownLatch latch = new CountDownLatch(1);
    private static AtomicLong NEXT_WRITE_ID = new AtomicLong(0);

    /**
     * Instantiates a new Transaction write future.
     *
     * @param sessionRequest the session request
     * @param logOperation   the log operation
     */
    public TransactionWriteFuture(SessionStorable sessionRequest, LogOperation logOperation) {
        this(sessionRequest, logOperation, DEFAULT_WRITE_TIMEOUT);
    }

    /**
     * Instantiates a new Transaction write future.
     *
     * @param sessionRequest the session request
     * @param logOperation   the log operation
     * @param timeoutMills   the timeout mills
     */
    public TransactionWriteFuture(SessionStorable sessionRequest, LogOperation logOperation, long timeoutMills) {
        this.writeId = NEXT_WRITE_ID.incrementAndGet();
        this.writeStore = new TransactionWriteStore(sessionRequest, logOperation);
        this.timeoutMills = timeoutMills;
    }

    /**
     * Is timeout boolean.
     *
     * @return the boolean
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - start > timeoutMills;
    }

    /**
     * Get boolean.
     *
     * @return the boolean
     * @throws InterruptedException the interrupted exception
     */
    public boolean get() throws InterruptedException {
        return get(timeoutMills, TimeUnit.MILLISECONDS);
    }

    /**
     * Get boolean.
     *
     * @param timeout the timeout
     * @param unit    the unit
     * @return the boolean
     * @throws InterruptedException the interrupted exception
     */
    public boolean get(long timeout, TimeUnit unit) throws InterruptedException {
        this.timeoutMills = unit.toMillis(timeout);
        boolean success = latch.await(timeout, unit);
        if (!success) {
            LOGGER.error("write file timeout,cost" + (System.currentTimeMillis() - start) + " ms");
            return false;
        }
        if (result instanceof Exception) {
            LOGGER.error("write file error,msg:" + ((Exception)result).getMessage());
            return false;
        }

        return (result instanceof Boolean) ? ((Boolean)result).booleanValue() : false;
    }

    /**
     * Sets result.
     *
     * @param result the result
     */
    public void setResult(Object result) {
        if (null != result) {
            this.result = result;
            latch.countDown();
        }
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public long getTimeoutMills() {
        return timeoutMills;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    public void setTimeoutMills(long timeout) {
        this.timeoutMills = timeout;
    }

    /**
     * Gets write id.
     *
     * @return the write id
     */
    public long getWriteId() {
        return writeId;
    }

    /**
     * Gets write store.
     *
     * @return the write store
     */
    public TransactionWriteStore getWriteStore() {
        return writeStore;
    }
}
