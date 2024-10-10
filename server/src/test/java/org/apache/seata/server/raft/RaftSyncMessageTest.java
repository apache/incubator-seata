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
package org.apache.seata.server.raft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.seata.common.exception.SeataRuntimeException;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.server.cluster.raft.snapshot.RaftSnapshot;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBranchSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftClusterMetadataMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftGlobalSessionSyncMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMessage;
import org.apache.seata.server.cluster.raft.sync.RaftSyncMessageSerializer;
import org.apache.seata.server.cluster.raft.snapshot.RaftSnapshotSerializer;
import org.apache.seata.server.cluster.raft.snapshot.session.RaftSessionSnapshot;
import org.apache.seata.server.cluster.raft.sync.msg.dto.BranchTransactionDTO;
import org.apache.seata.server.cluster.raft.sync.msg.dto.GlobalTransactionDTO;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHelper;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 */
@SpringBootTest
public class RaftSyncMessageTest {

    @BeforeAll
    public static void setUp(ApplicationContext context){
        SessionHolder.init(StoreConfig.SessionMode.FILE);
    }

    @AfterAll
    public static void destroy(){
        SessionHolder.destroy();
    }

    @Test
    public void testSecurityMsgSerialize() throws IOException {
        TestSecurity testSecurity = new TestSecurity();
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(testSecurity);
            bytes =  bos.toByteArray();
        }
        Assertions.assertThrows(SeataRuntimeException.class,()->RaftSyncMessageSerializer.decode(bytes));
    }

    @Test
    public void testMsgSerialize() throws IOException {
        RaftSyncMessage raftSyncMessage = new RaftSyncMessage();
        RaftGlobalSessionSyncMsg raftSessionSyncMsg = new RaftGlobalSessionSyncMsg();
        RaftBranchSessionSyncMsg raftBranchSessionMsg = new RaftBranchSessionSyncMsg();
        raftBranchSessionMsg.setBranchSession(new BranchTransactionDTO("123:123", 1234));
        raftSessionSyncMsg.setGlobalSession(new GlobalTransactionDTO("123:123"));
        raftSyncMessage.setBody(raftSessionSyncMsg);
        byte[] msg = RaftSyncMessageSerializer.encode(raftSyncMessage);
        RaftSyncMessage raftSyncMessage1 = RaftSyncMessageSerializer.decode(msg);
        RaftSyncMessage raftSyncMessage2 = new RaftSyncMessage();
        raftSyncMessage2.setBody(raftBranchSessionMsg);
        byte[] msg2 = RaftSyncMessageSerializer.encode(raftSyncMessage2);
        RaftSyncMessage raftSyncMessageByBranch = RaftSyncMessageSerializer.decode(msg2);
        Assertions.assertEquals("123:123", ((RaftBranchSessionSyncMsg) raftSyncMessageByBranch.getBody()).getBranchSession().getXid());
        Assertions.assertEquals("123:123", ((RaftGlobalSessionSyncMsg) raftSyncMessage1.getBody()).getGlobalSession().getXid());
        Assertions.assertEquals(1234, ((RaftBranchSessionSyncMsg) raftSyncMessageByBranch.getBody()).getBranchSession().getBranchId());
    }

    @Test
    public void testMsgSerializeCompatible() throws IOException {
        io.seata.server.cluster.raft.sync.msg.RaftSyncMessage raftSyncMessage = new io.seata.server.cluster.raft.sync.msg.RaftSyncMessage();
        RaftGlobalSessionSyncMsg raftSessionSyncMsg = new RaftGlobalSessionSyncMsg();
        RaftBranchSessionSyncMsg raftBranchSessionMsg = new RaftBranchSessionSyncMsg();
        raftBranchSessionMsg.setBranchSession(new BranchTransactionDTO("123:123", 1234));
        raftSessionSyncMsg.setGlobalSession(new GlobalTransactionDTO("123:123"));
        raftSyncMessage.setBody(raftSessionSyncMsg);
        byte[] msg = RaftSyncMessageSerializer.encode(raftSyncMessage);
        RaftSyncMessage raftSyncMessage1 = RaftSyncMessageSerializer.decode(msg);
        RaftSyncMessage raftSyncMessage2 = new RaftSyncMessage();
        raftSyncMessage2.setBody(raftBranchSessionMsg);
        byte[] msg2 = RaftSyncMessageSerializer.encode(raftSyncMessage2);
        RaftSyncMessage raftSyncMessageByBranch = RaftSyncMessageSerializer.decode(msg2);
        Assertions.assertEquals("123:123", ((RaftBranchSessionSyncMsg) raftSyncMessageByBranch.getBody()).getBranchSession().getXid());
        Assertions.assertEquals("123:123", ((RaftGlobalSessionSyncMsg) raftSyncMessage1.getBody()).getGlobalSession().getXid());
        Assertions.assertEquals(1234, ((RaftBranchSessionSyncMsg) raftSyncMessageByBranch.getBody()).getBranchSession().getBranchId());
    }

    @Test
    public void testSecuritySnapshotSerialize() throws IOException {
        TestSecurity testSecurity = new TestSecurity();
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(testSecurity);
            bytes =  bos.toByteArray();
        }
        Assertions.assertThrows(SeataRuntimeException.class,()->RaftSnapshotSerializer.decode(bytes));
    }

    @Test
    public void testSnapshotSerialize() throws IOException, TransactionException {
        Map<String, GlobalSession> sessionMap = new HashMap<>();
        GlobalSession globalSession = GlobalSession.createGlobalSession("123", "123", "123", 11111);
        sessionMap.put(globalSession.getXid(), globalSession);
        globalSession
                .addBranch(SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, "!23", null, "123", "123"));
        RaftSessionSnapshot sessionSnapshot = new RaftSessionSnapshot();
        sessionMap.forEach((xid, session) -> sessionSnapshot.convert2GlobalSessionByte(session));
        RaftSnapshot raftSnapshot = new RaftSnapshot();
        raftSnapshot.setBody(sessionSnapshot);
        byte[] msg = RaftSnapshotSerializer.encode(raftSnapshot);
        RaftSnapshot raftSnapshot1 = RaftSnapshotSerializer.decode(msg);
        RaftSessionSnapshot sessionSnapshot2 = (RaftSessionSnapshot)raftSnapshot1.getBody();
        Map<String, GlobalSession> map = sessionSnapshot2.convert2GlobalSession();
        Assertions.assertEquals(1, map.size());
        Assertions.assertNotNull(map.get(globalSession.getXid()));
        Assertions.assertEquals(1, map.get(globalSession.getXid()).getBranchSessions().size());
    }

    @Test
    public void testSnapshotCompatible() throws IOException, TransactionException {
        Map<String, GlobalSession> sessionMap = new HashMap<>();
        GlobalSession globalSession = GlobalSession.createGlobalSession("123", "123", "123", 11111);
        sessionMap.put(globalSession.getXid(), globalSession);
        globalSession
                .addBranch(SessionHelper.newBranchByGlobal(globalSession, BranchType.AT, "!23", null, "123", "123"));
        RaftSessionSnapshot sessionSnapshot = new RaftSessionSnapshot();
        sessionMap.forEach((xid, session) -> sessionSnapshot.convert2GlobalSessionByte(session));
        io.seata.server.cluster.raft.snapshot.RaftSnapshot raftSnapshot = new   io.seata.server.cluster.raft.snapshot.RaftSnapshot();
        raftSnapshot.setBody(sessionSnapshot);
        raftSnapshot.setType(io.seata.server.cluster.raft.snapshot.RaftSnapshot.SnapshotType.session);
        byte[] msg = RaftSnapshotSerializer.encode(raftSnapshot);
        RaftSnapshot raftSnapshot1 = RaftSnapshotSerializer.decode(msg);
        RaftSessionSnapshot sessionSnapshot2 = (RaftSessionSnapshot)raftSnapshot1.getBody();
        Map<String, GlobalSession> map = sessionSnapshot2.convert2GlobalSession();
        Assertions.assertEquals(1, map.size());
        Assertions.assertNotNull(map.get(globalSession.getXid()));
        Assertions.assertEquals(1, map.get(globalSession.getXid()).getBranchSessions().size());
    }

    @Test
    public void testRaftClusterMetadataSerialize() throws IOException {
        RaftSyncMessage raftSyncMessage = new RaftSyncMessage();
        RaftClusterMetadata raftClusterMetadata = new RaftClusterMetadata();
        //set leader
        Node leader = new Node();
        leader.setRole(ClusterRole.LEADER);
        leader.setGroup("abc");
        leader.setControl(leader.createEndpoint("1.1.1.1",8088,"http"));
        leader.setTransaction(leader.createEndpoint("1.1.1.1",8089,"netty"));
        Map<String,Object> metaData=new HashMap<>();
        metaData.put("abc","abc");
        leader.setMetadata(metaData);
        raftClusterMetadata.setLeader(leader);
        //set learner
        Node learner = new Node();
        learner.setRole(ClusterRole.LEARNER);
        learner.setGroup("abc");
        learner.setControl(leader.createEndpoint("1.1.1.2",8088,"http"));
        learner.setTransaction(leader.createEndpoint("1.1.1.2",8089,"netty"));
        List<Node> learners=new ArrayList<>();
        learners.add(learner);
        raftClusterMetadata.setLearner(learners);
        //set follower
        Node follower = new Node();
        follower.setRole(ClusterRole.FOLLOWER);
        follower.setGroup("abc");
        follower.setControl(leader.createEndpoint("1.1.1.3",8088,"http"));
        follower.setTransaction(leader.createEndpoint("1.1.1.3",8089,"netty"));
        List<Node> followers=new ArrayList<>();
        followers.add(follower);
        raftClusterMetadata.setFollowers(followers);

        RaftClusterMetadataMsg raftClusterMetadataMsg=new RaftClusterMetadataMsg(raftClusterMetadata);

        raftSyncMessage.setBody(raftClusterMetadataMsg);
        byte[] msg = RaftSyncMessageSerializer.encode(raftSyncMessage);
        RaftSyncMessage raftSyncMessage1 = RaftSyncMessageSerializer.decode(msg);
        RaftClusterMetadataMsg raftClusterMetadataMsg1=(RaftClusterMetadataMsg)  raftSyncMessage1.getBody();
        Node leader1=raftClusterMetadataMsg1.getRaftClusterMetadata().getLeader();
        Assertions.assertEquals("1.1.1.1", leader1.getControl().getHost());
        Assertions.assertEquals("abc",leader1.getMetadata().get("abc"));
        Assertions.assertEquals(1,raftClusterMetadataMsg1.getRaftClusterMetadata().getFollowers().size());
        Node follower1=raftClusterMetadataMsg1.getRaftClusterMetadata().getFollowers().get(0);
        Assertions.assertEquals("abc", follower1.getGroup());
        Assertions.assertEquals(1,raftClusterMetadataMsg1.getRaftClusterMetadata().getLearner().size());
        Node learner1=raftClusterMetadataMsg1.getRaftClusterMetadata().getLearner().get(0);
        Assertions.assertEquals(ClusterRole.LEARNER,learner1.getRole());
    }

}
