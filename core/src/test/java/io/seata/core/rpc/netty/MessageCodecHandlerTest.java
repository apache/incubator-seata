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
///*
// *  Copyright 1999-2019 Seata.io Group.
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// */
//package io.seata.core.rpc.netty;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.UnpooledByteBufAllocator;
//import io.netty.channel.ChannelHandlerContext;
//import io.seata.core.protocol.RpcMessage;
//import io.seata.core.protocol.transaction.GlobalBeginRequest;
//import org.junit.Ignore;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.fail;
//
//public class MessageCodecHandlerTest {
//
//    @Test
//    @Ignore("seata serialize can not support this way,only we can change serialize type dynamicly.")
//    public void encodeAndDecode() {
//
//        MessageCodecHandler messageCodecHandler = new MessageCodecHandler();
//        ByteBuf out = UnpooledByteBufAllocator.DEFAULT.directBuffer(1024);
//
//        GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
//        globalBeginRequest.setTransactionName("trans-1");
//        globalBeginRequest.setTimeout(3000);
//
//        RpcMessage msg = new RpcMessage();
//        msg.setId(1);
//        msg.setAsync(false);
//        msg.setHeartbeat(false);
//        msg.setRequest(true);
//        msg.setBody(globalBeginRequest);
//        ChannelHandlerContext ctx = null;
//        try {
//            messageCodecHandler.encode(ctx, msg, out);
//        } catch (Exception e) {
//            fail(e.getMessage());
//        }
//        List<Object> objetcs = new ArrayList<>();
//        try {
//            messageCodecHandler.decode(ctx, out, objetcs);
//        } catch (Exception e) {
//            fail(e.getMessage());
//        }
//
//        assertThat(objetcs.size()).isEqualTo(1);
//        final Object actual = objetcs.get(0);
//        assertThat(actual instanceof RpcMessage).isEqualTo(true);
//
//        RpcMessage rpcMessage = (RpcMessage)actual;
//
//        GlobalBeginRequest decodeGlobalBeginRequest = (GlobalBeginRequest)rpcMessage.getBody();
//        assertThat(decodeGlobalBeginRequest.getTransactionName()).isEqualTo(
//            globalBeginRequest.getTransactionName());
//        assertThat(decodeGlobalBeginRequest.getTimeout()).isEqualTo(globalBeginRequest.getTimeout());
//        assertThat(decodeGlobalBeginRequest.getTypeCode()).isEqualTo(globalBeginRequest.getTypeCode());
//
//    }
//
//}