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
package org.apache.seata.spring.boot.autoconfigure.properties.server;

import org.apache.seata.spring.boot.autoconfigure.properties.server.raft.ServerRaftProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerRaftPropertiesTest {

    @Test
    public void testServerRaftProperties() {
        ServerRaftProperties serverRaftProperties = new ServerRaftProperties();
        serverRaftProperties.setServerAddr("server");
        serverRaftProperties.setGroup("group");
        serverRaftProperties.setCompressor("compressor");
        serverRaftProperties.setSerialization("serialization");
        serverRaftProperties.setApplyBatch(1);
        serverRaftProperties.setDisruptorBufferSize(1);
        serverRaftProperties.setElectionTimeoutMs(1);
        serverRaftProperties.setMaxAppendBufferSize(1);
        serverRaftProperties.setMaxReplicatorInflightMsgs(1);
        serverRaftProperties.setReporterInitialDelay(1);
        serverRaftProperties.setSnapshotInterval(1);
        serverRaftProperties.setAutoJoin(true);
        serverRaftProperties.setReporterEnabled(true);
        serverRaftProperties.setSync(true);

        Assertions.assertEquals("server", serverRaftProperties.getServerAddr());
        Assertions.assertEquals("group", serverRaftProperties.getGroup());
        Assertions.assertEquals("compressor", serverRaftProperties.getCompressor());
        Assertions.assertEquals("serialization", serverRaftProperties.getSerialization());
        Assertions.assertEquals(1, serverRaftProperties.getApplyBatch());
        Assertions.assertEquals(1, serverRaftProperties.getDisruptorBufferSize());
        Assertions.assertEquals(1, serverRaftProperties.getElectionTimeoutMs());
        Assertions.assertEquals(1, serverRaftProperties.getMaxAppendBufferSize());
        Assertions.assertEquals(1, serverRaftProperties.getMaxReplicatorInflightMsgs());
        Assertions.assertEquals(1, serverRaftProperties.getReporterInitialDelay());
        Assertions.assertEquals(1, serverRaftProperties.getSnapshotInterval());
        Assertions.assertTrue(serverRaftProperties.getAutoJoin());
        Assertions.assertTrue(serverRaftProperties.isReporterEnabled());
        Assertions.assertTrue(serverRaftProperties.isSync());
    }
}
