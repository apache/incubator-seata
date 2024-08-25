package org.apache.seata.core.rpc.netty.grpc;

import com.google.protobuf.Any;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.ReferenceCounted;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.serializer.Serializer;
import org.apache.seata.core.serializer.SerializerServiceLoader;
import org.apache.seata.core.serializer.SerializerType;

public class GrpcDecoder extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http2HeadersFrame) {
            onHeadersRead(ctx, (Http2HeadersFrame) msg);
        } else if (msg instanceof Http2DataFrame) {
            onDataRead(ctx, (Http2DataFrame) msg);
        } else if (msg instanceof ReferenceCounted) {
            ctx.fireChannelRead(msg);
        }
    }

    public void onDataRead(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
        ByteBuf content = msg.content();
        int readableBytes = content.readableBytes();
        byte[] bytes = new byte[readableBytes];
        content.readBytes(bytes);
        if (bytes.length < 5) {
            return;
        }

        int srcPos = 0;
        while (srcPos < readableBytes) {
            // The first byte defaults to 0, indicating that no decompression is required
            // Read the value of the next four bytes as the length of the body
            int length = ((bytes[srcPos + 1] & 0xFF) << 24) |
                    ((bytes[srcPos + 2] & 0xFF) << 16) |
                    ((bytes[srcPos + 3] & 0xFF) << 8) |
                    (bytes[srcPos + 4] & 0xFF);

            byte[] data = new byte[length];
            System.arraycopy(bytes, srcPos + 5, data, 0, length);

            GrpcMessageProto grpcMessageProto = GrpcMessageProto.parseFrom(data);
            Any body = grpcMessageProto.getBody();
            int messageType = grpcMessageProto.getMessageType();
            int messageId = grpcMessageProto.getId();
            byte[] byteArray = body.toByteArray();

            Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(SerializerType.GRPC.getCode()));
            Object messageBody = serializer.deserialize(byteArray);

            RpcMessage rpcMsg = new RpcMessage();
            rpcMsg.setMessageType((byte) messageType);
            rpcMsg.setBody(messageBody);
            rpcMsg.setId(messageId);
            rpcMsg.setHeadMap(grpcMessageProto.getHeadMapMap());

            ctx.fireChannelRead(rpcMsg);

            srcPos += length + 5;
        }
    }

    public void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame) throws Exception {
        // TODO Subsequent decompression logic is possible
    }
}
