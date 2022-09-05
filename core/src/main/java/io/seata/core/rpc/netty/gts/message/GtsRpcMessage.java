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
package io.seata.core.rpc.netty.gts.message;

import java.util.concurrent.atomic.AtomicLong;

public class GtsRpcMessage {
    private static AtomicLong NEXT_ID = new AtomicLong(0L);
    private long id;
    private boolean isAsync;
    private boolean isRequest;
    private boolean isHeartbeat;
    private Object body;

    public GtsRpcMessage() {
    }

    public static long getNextMessageId() {
        return NEXT_ID.incrementAndGet();
    }

    public boolean isAsync() {
        return this.isAsync;
    }

    public void setAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }

    public boolean isRequest() {
        return this.isRequest;
    }

    public void setRequest(boolean isRequest) {
        this.isRequest = isRequest;
    }

    public boolean isHeartbeat() {
        return this.isHeartbeat;
    }

    public void setHeartbeat(boolean isHeartbeat) {
        this.isHeartbeat = isHeartbeat;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Object getBody() {
        return this.body;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
