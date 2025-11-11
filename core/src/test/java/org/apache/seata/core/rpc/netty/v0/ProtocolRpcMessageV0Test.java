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
package org.apache.seata.core.rpc.netty.v0;

import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.RpcMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProtocolRpcMessageV0Test {

    private ProtocolRpcMessageV0 message;

    @BeforeEach
    public void setUp() {
        message = new ProtocolRpcMessageV0();
    }

    @Test
    public void testGetNextMessageId() {
        long id1 = ProtocolRpcMessageV0.getNextMessageId();
        long id2 = ProtocolRpcMessageV0.getNextMessageId();
        Assertions.assertTrue(id2 > id1);
    }

    @Test
    public void testSetAndGetId() {
        message.setId(100L);
        Assertions.assertEquals(100L, message.getId());
    }

    @Test
    public void testSetAndGetAsync() {
        message.setAsync(true);
        Assertions.assertTrue(message.isAsync());

        message.setAsync(false);
        Assertions.assertFalse(message.isAsync());
    }

    @Test
    public void testSetAndGetRequest() {
        message.setRequest(true);
        Assertions.assertTrue(message.isRequest());

        message.setRequest(false);
        Assertions.assertFalse(message.isRequest());
    }

    @Test
    public void testSetAndGetHeartbeat() {
        message.setHeartbeat(true);
        Assertions.assertTrue(message.isHeartbeat());

        message.setHeartbeat(false);
        Assertions.assertFalse(message.isHeartbeat());
    }

    @Test
    public void testSetAndGetSeataCodec() {
        message.setSeataCodec(true);
        Assertions.assertTrue(message.isSeataCodec());

        message.setSeataCodec(false);
        Assertions.assertFalse(message.isSeataCodec());
    }

    @Test
    public void testSetAndGetBody() {
        Object body = new Object();
        message.setBody(body);
        Assertions.assertSame(body, message.getBody());
    }

    @Test
    public void testSetAndGetMessageType() {
        message.setMessageType((byte) MessageType.TYPE_HEARTBEAT_MSG);
        Assertions.assertEquals((byte) MessageType.TYPE_HEARTBEAT_MSG, message.getMessageType());
    }

    @Test
    public void testProtocolMsg2RpcMsg() {
        message.setId(123L);
        message.setAsync(true);
        message.setRequest(true);
        message.setHeartbeat(false);
        byte msgType = (byte) MessageType.TYPE_BRANCH_COMMIT;
        message.setMessageType(msgType);
        Object body = "test body";
        message.setBody(body);

        RpcMessage rpcMessage = message.protocolMsg2RpcMsg();

        Assertions.assertNotNull(rpcMessage);
        // V0 protocol converts specific message types to generic protocol types
        // When isRequest=true and isHeartbeat=false, it becomes MSGTYPE_RESQUEST_ONEWAY
        Assertions.assertEquals((byte) 2, rpcMessage.getMessageType());
        Assertions.assertSame(body, rpcMessage.getBody());
    }

    @Test
    public void testRpcMsg2ProtocolMsg() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(456);
        rpcMessage.setMessageType((byte) MessageType.TYPE_HEARTBEAT_MSG);
        Object body = "rpc body";
        rpcMessage.setBody(body);

        message.rpcMsg2ProtocolMsg(rpcMessage);

        Assertions.assertEquals(456L, message.getId());
        Assertions.assertEquals((byte) MessageType.TYPE_HEARTBEAT_MSG, message.getMessageType());
        Assertions.assertSame(body, message.getBody());
    }

    @Test
    public void testRoundTripConversion() {
        message.setId(789L);
        message.setAsync(true);
        message.setRequest(true);
        message.setHeartbeat(false);
        message.setSeataCodec(true);
        byte msgType = (byte) MessageType.TYPE_GLOBAL_COMMIT;
        message.setMessageType(msgType);
        String body = "round trip test";
        message.setBody(body);

        RpcMessage rpcMessage = message.protocolMsg2RpcMsg();

        ProtocolRpcMessageV0 newMessage = new ProtocolRpcMessageV0();
        newMessage.rpcMsg2ProtocolMsg(rpcMessage);

        // After round trip conversion, the messageType becomes the generic protocol type
        // When isRequest=true and isHeartbeat=false, it becomes MSGTYPE_RESQUEST_ONEWAY (2)
        Assertions.assertEquals((byte) 2, newMessage.getMessageType());
        Assertions.assertEquals(body, newMessage.getBody());
        Assertions.assertTrue(newMessage.isRequest());
        Assertions.assertFalse(newMessage.isHeartbeat());
    }

    @Test
    public void testMultipleMessageIdsAreUnique() {
        long id1 = ProtocolRpcMessageV0.getNextMessageId();
        long id2 = ProtocolRpcMessageV0.getNextMessageId();
        long id3 = ProtocolRpcMessageV0.getNextMessageId();

        Assertions.assertNotEquals(id1, id2);
        Assertions.assertNotEquals(id2, id3);
        Assertions.assertNotEquals(id1, id3);
    }
}
