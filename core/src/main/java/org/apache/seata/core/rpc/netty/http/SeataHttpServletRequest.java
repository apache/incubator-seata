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
package org.apache.seata.core.rpc.netty.http;

import javax.servlet.AsyncContext;

public class SeataHttpServletRequest extends BaseSeataServletRequest {

    private AsyncContext asyncContext;

    private String remoteAddr;

    public SeataHttpServletRequest(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        asyncContext = new SeataAsyncContext(this);
        return asyncContext;
    }

    @Override
    public boolean isAsyncStarted() {
        return asyncContext != null;
    }

    @Override
    public boolean isAsyncSupported() {
        return asyncContext != null;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return asyncContext;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }
}
