package org.apache.seata.core.rpc.netty.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.util.ReferenceCounted;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.serializer.protobuf.convertor.PbConvertor;
import org.apache.seata.core.serializer.protobuf.manager.ProtobufConvertManager;

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
        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);

        // 第一个字节默认是0，表示无需解压缩
        // 读取后面四个字节的值，作为body的长度
        int length = ((bytes[1] & 0xFF) << 24) |
                ((bytes[2] & 0xFF) << 16) |
                ((bytes[3] & 0xFF) << 8) |
                (bytes[4] & 0xFF);

        byte[] data = new byte[length];
        System.arraycopy(bytes, 5, data, 0, length);

        GrpcMessageProto grpcMessageProto = GrpcMessageProto.parseFrom(data);
        Any body = grpcMessageProto.getBody();
        int messageType = grpcMessageProto.getMessageType();
        int messageId = grpcMessageProto.getId();
        final Class clazz = ProtobufConvertManager.getInstance().fetchProtoClass(
                getTypeNameFromTypeUrl(body.getTypeUrl()));
        if (body.is(clazz)) {
            try {
                Object ob = body.unpack(clazz);
                final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchReversedConvertor(clazz.getName());
                Object model = pbConvertor.convert2Model(ob);

                RpcMessage rpcMsg = new RpcMessage();
                rpcMsg.setMessageType((byte) messageType);
                rpcMsg.setBody(model);
                rpcMsg.setId(messageId);
                rpcMsg.setHeadMap(grpcMessageProto.getHeadMapMap());

                ctx.fireChannelRead(rpcMsg);
            } catch (InvalidProtocolBufferException e) {
                throw new ShouldNeverHappenException(e);
            }
        }
    }

    public void onHeadersRead(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame) throws Exception {
        // TODO 后续可以解压缩逻辑
    }

    private String getTypeNameFromTypeUrl(String typeUri) {
        int pos = typeUri.lastIndexOf('/');
        return pos == -1 ? "" : typeUri.substring(pos + 1);
    }

}
