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
package com.alibaba.fescar.core.rpc.netty;

import java.util.concurrent.ThreadFactory;

import io.netty.channel.EventLoopGroup;

/**
 * The interface Rpc event loop group.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/12
 */
public interface RpcEventLoopGroup {

    // EventLoopGroup WORKER_GROUP = new RpcEventLoopGroup() {
    //    @Override
    //    public EventLoopGroup createEventLoopGroup(int workThreadSize, ThreadFactory threadFactory) {
    //        return null;
    //    }
    //};

    /**
     * Create event loop group event loop group.
     *
     * @param workThreadSize the work thread size
     * @param threadFactory  the thread factory
     * @return the event loop group
     */
    EventLoopGroup createEventLoopGroup(int workThreadSize, ThreadFactory threadFactory);
}
