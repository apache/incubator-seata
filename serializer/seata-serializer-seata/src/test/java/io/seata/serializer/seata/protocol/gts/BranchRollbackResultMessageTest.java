package io.seata.serializer.seata.protocol.gts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.seata.core.rpc.netty.gts.message.BranchRollbackMessage;
import io.seata.core.rpc.netty.gts.message.BranchRollbackResultMessage;
import io.seata.core.rpc.netty.gts.message.GtsRpcMessage;
import io.seata.core.rpc.netty.gts.message.TxcCodec;
import io.seata.core.rpc.netty.v1.ProtocolV1Decoder;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class BranchRollbackResultMessageTest {
    private static short MAGIC = -9510;

    @Test
    public void testBranchRollbackResultMessage(){
        BranchRollbackResultMessage msg = new BranchRollbackResultMessage();
        msg.setTranId(347710542L);
        msg.setBranchId(347710544L);


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
