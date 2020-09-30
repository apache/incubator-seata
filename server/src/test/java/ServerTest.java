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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.server.raft.RaftServer;
import org.junit.jupiter.api.Test;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.Task;

import io.seata.common.XID;
import io.seata.common.util.NetUtil;
import io.seata.config.ConfigurationChangeEvent;
import io.seata.core.rpc.netty.NettyRemotingServer;
import io.seata.server.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.raft.RaftServerFactory;
import io.seata.server.storage.raft.RaftSyncMsg;

import static com.alipay.remoting.serialization.SerializerManager.Hessian2;

/**
 * The type Server test.
 *
 * @author slievrly
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
        nettyServer.setHandler(new DefaultCoordinator(nettyServer));
        UUIDGenerator.init(1L);
        XID.setIpAddress(NetUtil.getLocalIp());
        XID.setPort(nettyServer.getListenPort());
        nettyServer.init();
        System.exit(0);
    }

    @Test
    public void testRaftCluster() throws Exception {
        try {
            RaftServerFactory raftServerFactory = new RaftServerFactory();
            String ip = "127.0.0.1";
            String conf = "127.0.0.1:7091,127.0.0.1:7092,127.0.0.1:7093";
            raftServerFactory.init(ip, 8091, conf);
            RaftServer raftServer = raftServerFactory.getRaftServer();
            raftServerFactory.init(ip, 8092, conf);
            RaftServer raftServer2 = raftServerFactory.getRaftServer();
            raftServerFactory.init(ip, 8093, conf);
            RaftServer raftServer3 = raftServerFactory.getRaftServer();
            Task task = new Task();
            RaftSyncMsg raftSyncMsg = new RaftSyncMsg();
            PeerId peerId;
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                peerId = raftServer.getNode().getLeaderId();
                if (peerId != null) {
                    break;
                }
            }
            RaftServer leader = null;
            switch (peerId.getPort()) {
                case 7091:
                    leader = raftServer;
                    break;
                case 7092:
                    leader = raftServer2;
                    break;
                case 7093:
                    leader = raftServer3;
                    break;
                default:
                    break;
            }
            RaftServerFactory.getInstance().setRaftServer(leader);
            RaftServerFactory.getInstance().setStateMachine(leader.getRaftStateMachine());
            try {
                task.setData(ByteBuffer.wrap(SerializerManager.getSerializer(Hessian2).serialize(raftSyncMsg)));
            } catch (CodecException e) {
                e.printStackTrace();
            }
            RaftServerFactory.getInstance().getRaftServer().getNode().apply(task);
            ConfigurationChangeEvent event = new ConfigurationChangeEvent();
            conf = conf + ",127.0.0.1:7094";
            raftServerFactory.init(ip, 8094, conf);
            RaftServer raftServer4 = raftServerFactory.getRaftServer();
            event.setNewValue(conf);
            raftServer.onChangeEvent(event);
            raftServer2.onChangeEvent(event);
            raftServer3.onChangeEvent(event);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            raftServer.onShutDown();
            raftServer2.onShutDown();
            raftServer3.onShutDown();
            raftServer4.onShutDown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
