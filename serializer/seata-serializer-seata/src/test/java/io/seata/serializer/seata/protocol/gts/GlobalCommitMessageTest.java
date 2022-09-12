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
package io.seata.serializer.seata.protocol.gts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.seata.core.rpc.netty.gts.message.GlobalCommitMessage;
import io.seata.core.rpc.netty.gts.message.GtsRpcMessage;
import io.seata.core.rpc.netty.gts.message.TxcCodec;
import io.seata.core.rpc.netty.v1.ProtocolV1Decoder;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class GlobalCommitMessageTest {

    private static short MAGIC = -9510;

    @Test
    public void testGlobalCommitMessage(){
        GlobalCommitMessage msg = new GlobalCommitMessage();
        msg.setTranId(347723853L);


        System.out.println(msg.toString());
        GtsRpcMessage rpcMessage = new GtsRpcMessage();
        rpcMessage.setId(1L);
        rpcMessage.setBody(msg);
        TxcCodec txcCodec = null;
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        if (rpcMessage.getBody() instanceof TxcCodec) {
            txcCodec = (TxcCodec)rpcMessage.getBody();
        }
        byteBuffer.putShort(MAGIC);
        int flag = (rpcMessage.isAsync() ? 64 : 0) | (rpcMessage.isHeartbeat() ? 32 : 0) | (rpcMessage.isRequest() ? 128 : 0) | (txcCodec != null ? 16 : 0);
        byteBuffer.putShort((short) flag);
        byte[] content;
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer(128);
        byteBuffer.putShort(txcCodec.getTypeCode());
        byteBuffer.putLong(rpcMessage.getId());
        byteBuffer.flip();
        content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        out.writeBytes(content);
        out.writeBytes(txcCodec.encode());
        ProtocolV1Decoder decoder = new ProtocolV1Decoder();
        try {
            decoder.decodeGts(msg.ctx, out);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
