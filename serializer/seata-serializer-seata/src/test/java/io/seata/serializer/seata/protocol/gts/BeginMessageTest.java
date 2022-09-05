package io.seata.serializer.seata.protocol.gts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.seata.core.rpc.netty.gts.message.BeginMessage;
import io.seata.core.rpc.netty.gts.message.GtsRpcMessage;
import io.seata.core.rpc.netty.gts.message.TxcCodec;
import io.seata.core.rpc.netty.v1.ProtocolV1Decoder;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

/**
 * @author cj3
 */
public class BeginMessageTest {
    private static short MAGIC = -9510;

    @Test
    public void testBeginMessage(){
        BeginMessage msg = new BeginMessage();
        msg.setAppname("default");
        msg.setTxcInst("com.taobao.txc.sample.BizService.doTransfer");
        System.out.println(msg.toString());
        byte[] bs = msg.encode();
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
