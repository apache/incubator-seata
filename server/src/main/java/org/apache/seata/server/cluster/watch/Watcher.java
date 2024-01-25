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
package org.apache.seata.server.cluster.watch;

import static org.apache.seata.server.cluster.watch.Watcher.Protocol.HTTP;

/**
 */
public class Watcher<T> {

    private String group;

    private volatile boolean done = false;

    private T asyncContext;

    private long timeout;

    private long term;

    private String protocol = HTTP;

    public Watcher(String group, T asyncContext, int timeout, long term) {
        this.group = group;
        this.asyncContext = asyncContext;
        this.timeout = System.currentTimeMillis() + timeout;
        this.term = term;
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

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public interface Protocol {
        String GRPC = "grpc";
        String HTTP = "http";
    }

}
