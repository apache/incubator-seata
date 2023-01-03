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
package io.seata.server.cluster.watch;

import static io.seata.server.cluster.watch.Watcher.Protocol.HTTP;

/**
 * @author jianbin.chen
 */
public class Watcher<T> {

    private String group;

    private volatile boolean done = false;

    private T asyncContext;

    private int timeout;

    private long createTime = System.currentTimeMillis();

    private String protocol = HTTP;

    public Watcher(String group, T asyncContext, int timeout) {
        this.group = group;
        this.asyncContext = asyncContext;
        this.timeout = timeout;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public T getAsyncContext() {
        return asyncContext;
    }

    public void setAsyncContext(T asyncContext) {
        this.asyncContext = asyncContext;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public interface Protocol {
        String GRPC = "grpc";
        String HTTP = "http";
    }

}
