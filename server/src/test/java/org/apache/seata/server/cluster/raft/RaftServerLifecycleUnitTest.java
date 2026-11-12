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
package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.*;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RpcServer;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.ServerRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.util.*;

import static org.apache.seata.common.ConfigurationKeys.SERVER_RAFT_SSL_ENABLED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoBean(types = ServerRunner.class)
class RaftServerLifecycleUnitTest extends BaseSpringBootTest {
    @TempDir
    Path directory;

    @Test
    void startupConfiguresStorageSslAndShutdownJoinsGroup() throws Exception {
        Class.forName(RaftStateMachine.class.getName());
        String[] names = {
            "bolt.server.ssl.enable",
            "bolt.server.ssl.clientAuth",
            "bolt.client.ssl.enable",
            "bolt.server.ssl.keystore",
            "bolt.server.ssl.keystore.password",
            "bolt.server.ssl.keystore.type",
            "bolt.server.ssl.kmf.algorithm",
            "bolt.client.ssl.keystore",
            "bolt.client.ssl.keystore.password",
            "bolt.client.ssl.keystore.type",
            "bolt.client.ssl.tmf.algorithm"
        };
        Map<String, String> previous = new HashMap<>();
        for (String name : names) {
            previous.put(name, System.getProperty(name));
        }
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(eq(SERVER_RAFT_SSL_ENABLED), anyBoolean())).thenReturn(true);
        when(config.getConfig(anyString())).thenReturn("fixture");
        NodeOptions options = new NodeOptions();
        Node node = mock(Node.class);
        when(node.getOptions()).thenReturn(options);
        RouteTable routes = mock(RouteTable.class);
        try (MockedStatic<ConfigurationFactory> factory = mockStatic(ConfigurationFactory.class);
                MockedStatic<RouteTable> table = mockStatic(RouteTable.class);
                MockedConstruction<RaftStateMachine> machines = mockConstruction(RaftStateMachine.class);
                MockedConstruction<RaftGroupService> groups =
                        mockConstruction(RaftGroupService.class, (group, context) -> when(group.start(false))
                                .thenReturn(node))) {
            factory.when(ConfigurationFactory::getInstance).thenReturn(config);
            table.when(RouteTable::getInstance).thenReturn(routes);
            RaftServer server = new RaftServer(
                    directory.toString(), "group", new PeerId("127.0.0.1", 9091), options, mock(RpcServer.class));
            server.start();
            assertTrue(options.getLogUri().endsWith("group/log"));
            assertTrue(options.getSnapshotUri().endsWith("group/snapshot"));
            assertSame(node, server.getNode());
            assertEquals("true", System.getProperty("bolt.server.ssl.clientAuth"));
            assertEquals("fixture", System.getProperty("bolt.client.ssl.keystore"));
            server.close();
            verify(groups.constructed().get(0)).shutdown();
            verify(groups.constructed().get(0)).join();
            doThrow(new InterruptedException("interrupted"))
                    .when(groups.constructed().get(0))
                    .join();
            assertDoesNotThrow(server::destroy);
        } finally {
            previous.forEach((key, value) -> {
                if (value == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, value);
                }
            });
        }
    }
}
