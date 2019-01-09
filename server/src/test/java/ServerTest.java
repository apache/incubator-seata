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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.alibaba.fescar.core.rpc.netty.RpcServer;
import com.alibaba.fescar.server.UUIDGenerator;
import com.alibaba.fescar.server.coordinator.DefaultCoordinator;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/12/4 19:34
 * @FileName: ServerTest
 * @Description:
 */
public class ServerTest {

    private static final ThreadPoolExecutor workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
            new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());

    public static void main(String[] args) {

        RpcServer rpcServer = new RpcServer(workingThreads);
        rpcServer.setHandler(new DefaultCoordinator(rpcServer));
        UUIDGenerator.init(1);
        rpcServer.init();

        System.exit(0);
    }

}
