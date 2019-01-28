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

import java.util.concurrent.atomic.AtomicLong;

/**
 * The type Rpc message.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /9/14 15:00
 * @FileName: RpcMessage
 * @Description:
 */
public class RpcMessage {

    private static AtomicLong NEXT_ID = new AtomicLong(0);

    /**
     * Gets next message id.
     *
     * @return the next message id
     */
    public static  long getNextMessageId() {
        return NEXT_ID.incrementAndGet();
    }
    private long id;
    private boolean isAsync;
    private boolean isRequest;
    private boolean isHeartbeat;
    private Object body;

    /**
     * Gets id.
     *
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Is async boolean.
     *
     * @return the boolean
     */
    public boolean isAsync() {
        return isAsync;
    }

    /**
     * Sets async.
     *
     * @param async the async
     */
    public void setAsync(boolean async) {
        isAsync = async;
    }

    /**
     * Is request boolean.
     *
     * @return the boolean
     */
    public boolean isRequest() {
        return isRequest;
    }

    /**
     * Sets request.
     *
     * @param request the request
     */
    public void setRequest(boolean request) {
        isRequest = request;
    }

    /**
     * Is heartbeat boolean.
     *
     * @return the boolean
     */
    public boolean isHeartbeat() {
        return isHeartbeat;
    }

    /**
     * Sets heartbeat.
     *
     * @param heartbeat the heartbeat
     */
    public void setHeartbeat(boolean heartbeat) {
        isHeartbeat = heartbeat;
    }

    /**
     * Gets body.
     *
     * @return the body
     */
    public Object getBody() {
        return body;
    }

    /**
     * Sets body.
     *
     * @param body the body
     */
    public void setBody(Object body) {
        this.body = body;
    }
}
