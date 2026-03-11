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

package com.alibaba.fescar.core.protocol;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The type Message future.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /10/9 14:35
 * @FileName: MessageFuture
 * @Description:
 */
public class MessageFuture {
    private RpcMessage requestMessage;
    private long timeout;
    private long start = System.currentTimeMillis();
    private volatile Object resultMessage;
    private static final Object NULL = new Object();
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * Is timeout boolean.
     *
     * @return the boolean
     */
    public boolean isTimeout() {
        return System.currentTimeMillis() - start > timeout;
    }

    /**
     * Get object.
     *
     * @param timeout the timeout
     * @param unit    the unit
     * @return the object
     * @throws TimeoutException the timeout exception
     * @throws InterruptedException the interrupted exception
     */
    public Object get(long timeout, TimeUnit unit) throws TimeoutException,
        InterruptedException {
        boolean success = latch.await(timeout, unit);
        if (!success) {
            throw new TimeoutException("cost " + (System.currentTimeMillis() - start) + " ms");
        }

        if (resultMessage instanceof RuntimeException) {
            throw (RuntimeException)resultMessage;
        } else if (resultMessage instanceof Throwable) {
            throw new RuntimeException((Throwable)resultMessage);
        }

        return resultMessage;
    }

    /**
     * Sets result message.
     *
     * @param obj the obj
     */
    public void setResultMessage(Object obj) {
        this.resultMessage = (obj == null ? NULL : obj);
        latch.countDown();
    }

    /**
     * Gets request message.
     *
     * @return the request message
     */
    public RpcMessage getRequestMessage() {
        return requestMessage;
    }

    /**
     * Sets request message.
     *
     * @param requestMessage the request message
     */
    public void setRequestMessage(RpcMessage requestMessage) {
        this.requestMessage = requestMessage;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Sets timeout.
     *
     * @param timeout the timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
