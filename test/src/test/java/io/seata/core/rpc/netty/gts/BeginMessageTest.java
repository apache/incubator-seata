package io.seata.core.rpc.netty.gts;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.seata.core.compressor.Compressor;
import io.seata.core.compressor.CompressorFactory;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.rpc.netty.gts.message.*;
import io.seata.core.rpc.netty.v1.HeadMapSerializer;
import io.seata.core.serializer.Serializer;
import io.seata.core.serializer.SerializerServiceLoader;
import io.seata.core.serializer.SerializerType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeginMessageTest {

    private static short MAGIC = -9510;
    private static int HEAD_LENGHT = 14;
    private static final int FLAG_REQUEST = 128;
    private static final int FLAG_ASYNC = 64;
    private static final int FLAG_HEARTBEAT = 32;
    private static final int FLAG_TXCCODEC = 16;

    @Test
    public void testBeginMessage(){
        BeginMessage msg = new BeginMessage();
        msg.setAppname("GTS APP");
        msg.setTxcInst("GTS instance");
        System.out.println(msg.toString());
        byte[] bs = msg.encode();
        GtsRpcMessage gtsRpcMessage = new GtsRpcMessage();
        gtsRpcMessage.setId(1L);
        gtsRpcMessage.setBody(msg);
        TxcCodec txcCodec = null;
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        if (gtsRpcMessage.getBody() instanceof TxcCodec) {
            txcCodec = (TxcCodec)gtsRpcMessage.getBody();
        }
        byteBuffer.putShort(MAGIC);
        int flag = (gtsRpcMessage.isAsync() ? 64 : 0) | (gtsRpcMessage.isHeartbeat() ? 32 : 0) | (gtsRpcMessage.isRequest() ? 128 : 0) | (txcCodec != null ? 16 : 0);
        byteBuffer.putShort((short) flag);
        byte[] content;
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer(128);
        byteBuffer.putShort(txcCodec.getTypeCode());
        byteBuffer.putLong(gtsRpcMessage.getId());
        byteBuffer.flip();
        content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        out.writeBytes(content);
        out.writeBytes(txcCodec.encode());
        System.out.println(out);
        try {
            decode(out, new ArrayList<Object>());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            decodeFrame(GtsToSeata(out));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void decode(ByteBuf in, List<Object> out) throws Exception {

        int readableBytes = in.readableBytes();
        if (readableBytes >= HEAD_LENGHT) {
            int begin = in.readerIndex();
            byte[] buffer = new byte[HEAD_LENGHT];
            in.readBytes(buffer);
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            short magic = byteBuffer.getShort();
            if (magic != MAGIC) {
                return;
            } else {
                int flag = byteBuffer.getShort();
                boolean isHeartbeat = (32 & flag) > 0;
                boolean isRequest = (128 & flag) > 0;
                boolean isTxcCodec = (16 & flag) > 0;
                short bodyLength = 0;
                short typeCode = 0;
                if (!isTxcCodec) {
                    bodyLength = byteBuffer.getShort();
                } else {
                    typeCode = byteBuffer.getShort();
                }

                long msgId = byteBuffer.getLong();
                GtsRpcMessage gtsRpcMessage;
                if (isHeartbeat) {

                } else if (bodyLength > 0 && in.readableBytes() < bodyLength) {
                    in.readerIndex(begin);
                } else {
                    gtsRpcMessage = new GtsRpcMessage();
                    gtsRpcMessage.setId(msgId);
                    gtsRpcMessage.setAsync((64 & flag) > 0);
                    gtsRpcMessage.setHeartbeat(false);
                    gtsRpcMessage.setRequest(isRequest);
                    try {
                        if (isTxcCodec) {
                            TxcCodec codec = new BeginMessage();
                            if (!codec.decode(in)) {
                                BeginMessage beginMessage = (BeginMessage)codec;
                                in.readerIndex(HEAD_LENGHT);
                                int len = in.readableBytes();
                                byte[] bytes = new byte[len];
                                in.readBytes(bytes);
                                beginMessage.decode(ByteBuffer.wrap(bytes));
                                gtsRpcMessage.setBody(beginMessage);
                                return;
                            }
                        }
                    } catch (Exception var20) {
                        throw var20;
                    }
                    out.add(gtsRpcMessage);
                }
            }
        }
    }

    public Object decodeFrame(ByteBuf frame) throws Exception{
        byte b0 = frame.readByte();
        byte b1 = frame.readByte();
        if (ProtocolConstants.MAGIC_CODE_BYTES[0] != b0
                || ProtocolConstants.MAGIC_CODE_BYTES[1] != b1) {
            throw new IllegalArgumentException("Unknown magic code: " + b0 + ", " + b1);
        }

        byte version = frame.readByte();
        // TODO  check version compatible here

        int fullLength = frame.readInt();
        short headLength = frame.readShort();
        byte messageType = frame.readByte();
        byte codecType = frame.readByte();
        byte compressorType = frame.readByte();
        int requestId = frame.readInt();

        io.seata.core.protocol.RpcMessage rpcMessage = new io.seata.core.protocol.RpcMessage();
        rpcMessage.setCodec(codecType);
        rpcMessage.setId(requestId);
        rpcMessage.setCompressor(compressorType);
        rpcMessage.setMessageType(messageType);

        // direct read head with zero-copy
        int headMapLength = headLength - ProtocolConstants.V1_HEAD_LENGTH;
        if (headMapLength > 0) {
            Map<String, String> map = HeadMapSerializer.getInstance().decode(frame, headMapLength);
            rpcMessage.getHeadMap().putAll(map);
        }

        // read body
        if (messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST) {
            rpcMessage.setBody(HeartbeatMessage.PING);
        } else if (messageType == ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE) {
            rpcMessage.setBody(HeartbeatMessage.PONG);
        } else {
            int bodyLength = fullLength - headLength;
            if (bodyLength > 0) {
                byte[] bs = new byte[bodyLength];
                frame.readBytes(bs);
                Compressor compressor = CompressorFactory.getCompressor(compressorType);
                bs = compressor.decompress(bs);
                rpcMessage.setCodec((byte)0x1);

                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(2));
                rpcMessage.setBody(serializer.deserialize(bs));
                rpcMessage.setBody(bs);
            }
        }

        return rpcMessage;
    }

    //seata需要将gts协议转换
    protected ByteBuf GtsToSeata(ByteBuf in) throws Exception{
        in.resetReaderIndex();
        int readableBytes = in.readableBytes();
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer(128);
        int fullLength = ProtocolConstants.V1_HEAD_LENGTH;
        int headLength = ProtocolConstants.V1_HEAD_LENGTH;
        if(readableBytes > HEAD_LENGHT) {
            int begin = in.readerIndex();
            byte[] buffer = new byte[HEAD_LENGHT];
            in.readBytes(buffer);
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            short magic = byteBuffer.getShort();
            if (magic != MAGIC) {
                throw new RuntimeException();
            } else {
                out.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
                out.writeByte(ProtocolConstants.VERSION);
                // full Length(4B) and head length(2B) will fix in the end.
                out.writerIndex(out.writerIndex() + 6);
                int flag = byteBuffer.getShort();
                boolean isHeartbeat = (32 & flag) > 0;
                boolean isRequest = (128 & flag) > 0;
                boolean isTxcCodec = (16 & flag) > 0;
                short bodyLength = 0;
                short typeCode = 0;
                if (!isTxcCodec) {
                    bodyLength = byteBuffer.getShort();
                } else {
                    typeCode = byteBuffer.getShort();
                }
                long msgId = byteBuffer.getLong();
                //处理body适配逻辑
                out.writeByte(1);  //因为gts协议的类型有22种，比较多 先用1吧 Msg Type 考虑将类型放在Map里
                out.writeByte(0);  //Seaializer 1 byte
                out.writeByte(0);  //Compress 1 byte
                out.writeInt((int)msgId);  //id 4 byte
                Map<String, String> headMap = new HashMap<>();
                headMap.put("type", String.valueOf(typeCode));
                int headMapBytesLength = HeadMapSerializer.getInstance().encode(headMap, out);
                headLength += headMapBytesLength;
                fullLength += headMapBytesLength;
                int len = in.readableBytes();
                byte[] bytes = new byte[len];
                in.readBytes(bytes);

                //此处有疑问 如何将gts的消息类型和seata的消息类型进行映射 不妨用map
                GlobalBeginRequest msg = new GlobalBeginRequest();
                msg.setTransactionName("gtsToSeata");
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(2));
                byte[] serialize = serializer.serialize(msg);

                out.writeBytes(serialize); // skip header map, direct writeBody

                fullLength += serialize.length;
                // fix fullLength and headLength
                int writeIndex = out.writerIndex();
                // skip magic code(2B) + version(1B)
                out.writerIndex(writeIndex - fullLength + 3);
                out.writeInt(fullLength);
                out.writeShort(headLength);
                out.writerIndex(writeIndex);
            }
        }
        return out;
    }

    public ByteBuf GtsMessageToSeataMessage(ByteBuf in, ByteBuf out) {
        return null;
    }

}
