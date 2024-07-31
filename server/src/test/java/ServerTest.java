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
import org.apache.seata.common.XID;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.rpc.netty.NettyRemotingServer;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.server.coordinator.DefaultCoordinator;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The type Server test.
 *
 */
public class ServerTest {

    private static final ThreadPoolExecutor workingThreads = new ThreadPoolExecutor(100, 500, 500, TimeUnit.SECONDS,
            new LinkedBlockingQueue(20000), new ThreadPoolExecutor.CallerRunsPolicy());


    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        NettyRemotingServer nettyServer = new NettyRemotingServer(workingThreads);
        nettyServer.setHandler(DefaultCoordinator.getInstance(nettyServer));
        UUIDGenerator.init(1L);
        XID.setIpAddress(NetUtil.getLocalIp());
        XID.setPort(nettyServer.getListenPort());
        nettyServer.init();
        System.exit(0);
    }

}
