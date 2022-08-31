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
package io.seata.core.rpc.netty.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.seata.core.exception.DecodeException;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.rpc.netty.gts.exception.TxcException;
import io.seata.core.rpc.netty.gts.message.*;
import io.seata.core.serializer.Serializer;
import io.seata.core.compressor.Compressor;
import io.seata.core.compressor.CompressorFactory;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.serializer.SerializerServiceLoader;
import io.seata.core.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 0     1     2     3     4     5     6     7     8     9    10     11    12    13    14    15    16
 * +-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+
 * |   magic   |Proto|     Full length       |    Head   | Msg |Seria|Compr|     RequestId         |
 * |   code    |colVer|    (head+body)      |   Length  |Type |lizer|ess  |                       |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+-----------+
 * |                                                                                               |
 * |                                   Head Map [Optional]                                         |
 * +-----------+-----------+-----------+-----------+-----------+-----------+-----------+-----------+
 * |                                                                                               |
 * |                                         body                                                  |
 * |                                                                                               |
 * |                                        ... ...                                                |
 * +-----------------------------------------------------------------------------------------------+
 * </pre>
 * <p>
 * <li>Full Length: include all data </li>
 * <li>Head Length: include head data from magic code to head map. </li>
 * <li>Body Length: Full Length - Head Length</li>
 * </p>
 * https://github.com/seata/seata/issues/893
 *
 * @author Geng Zhang
 * @see ProtocolV1Encoder
 * @since 0.7.0
 */
public class ProtocolV1Decoder extends LengthFieldBasedFrameDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolV1Decoder.class);

    private static short MAGIC = -9510;
    private static int HEAD_LENGHT = 14;
    private static final int FLAG_REQUEST = 128;
    private static final int FLAG_ASYNC = 64;
    private static final int FLAG_HEARTBEAT = 32;
    private static final int FLAG_TXCCODEC = 16;

    public ProtocolV1Decoder() {
        // default is 8M
        this(ProtocolConstants.MAX_FRAME_LENGTH);
    }

    public ProtocolV1Decoder(int maxFrameLength) {
        /*
        int maxFrameLength,
        int lengthFieldOffset,  magic code is 2B, and version is 1B, and then FullLength. so value is 3
        int lengthFieldLength,  FullLength is int(4B). so values is 4
        int lengthAdjustment,   FullLength include all data and read 7 bytes before, so the left length is (FullLength-7). so values is -7
        int initialBytesToStrip we will check magic code and version self, so do not strip any bytes. so values is 0
        */
        super(maxFrameLength, 3, 4, -7, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded;
        in.markReaderIndex();
        try {
            decoded = super.decode(ctx, in);
            if (decoded instanceof ByteBuf) {
                ByteBuf frame = (ByteBuf) decoded;
                frame.markReaderIndex();
                byte b0 = frame.readByte();
                byte b1 = frame.readByte();
                byte b2 = frame.readByte();
                frame.resetReaderIndex();
                if (ProtocolConstants.MAGIC_CODE_BYTES[0] == b0
                        && ProtocolConstants.MAGIC_CODE_BYTES[1] == b1) {
                    // seata message
                    if(b2 == ProtocolConstants.VERSION) {
                        try {
                            return decodeFrame(frame);
                        } finally {
                            frame.release();
                        }
                    } else if (b2 == (byte) 0) { // gts message
                        try {
                            decodeGts(ctx, frame);
                        } finally {
                            frame.release();
                        }
                    } else {
                        frame.release();
                    }
                }  else {
                    frame.release();
                }
            }
        } catch (Exception exx) {
            LOGGER.error("Decode frame error, cause: {}", exx.getMessage());
            throw new DecodeException(exx);
        }
        return decoded;
    }

    public Object decodeFrame(ByteBuf frame) {
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

        RpcMessage rpcMessage = new RpcMessage();
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
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(rpcMessage.getCodec()));
                rpcMessage.setBody(serializer.deserialize(bs));
            }
        }

        return rpcMessage;
    }

    public void decodeGts(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("channel:" + ctx.channel());
        }
        int readableBytes = in.readableBytes();
        if (readableBytes >= HEAD_LENGHT) {
            int begin = in.readerIndex();
            byte[] buffer = new byte[HEAD_LENGHT];
            in.readBytes(buffer);
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            short magic = byteBuffer.getShort();
            if (magic != MAGIC) {
                ctx.channel().close();
            } else {
                short flag = byteBuffer.getShort();
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
                    gtsRpcMessage = new GtsRpcMessage();
                    gtsRpcMessage.setId(msgId);
                    gtsRpcMessage.setAsync(true);
                    gtsRpcMessage.setHeartbeat(isHeartbeat);
                    gtsRpcMessage.setRequest(isRequest);
                    if (isRequest) {
                        gtsRpcMessage.setBody(HeartbeatMessage.PING);
                    } else {
                        gtsRpcMessage.setBody(HeartbeatMessage.PONG);
                    }

                } else if (bodyLength > 0 && in.readableBytes() < bodyLength) {
                    in.readerIndex(begin);
                } else {
                    gtsRpcMessage = new GtsRpcMessage();
                    gtsRpcMessage.setId(msgId);
                    gtsRpcMessage.setAsync((64 & flag) > 0);
                    gtsRpcMessage.setHeartbeat(false);
                    gtsRpcMessage.setRequest(isRequest);

                    //Seata protocal head
                    ByteBuf seataOut = ByteBufAllocator.DEFAULT.buffer(128);
                    int fullLength = ProtocolConstants.V1_HEAD_LENGTH;
                    int headLength = ProtocolConstants.V1_HEAD_LENGTH;
                    seataOut.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
                    seataOut.writeByte(ProtocolConstants.VERSION);
                    seataOut.writerIndex(seataOut.writerIndex() + 6);

                    try {
                        if (isTxcCodec) {
                            TxcCodec codec = this.getTxcCodecInstance(typeCode);
                            codec.setChannelHandlerContext(ctx);
                            if (!codec.decode(in)) {
                                in.readerIndex(begin);
                                throw new Exception("gts message format exception");
                            }

                            // 转换协议
                            byte[] msgOut = null;
                            Object seataCodec = this.changetoSeataCodec(typeCode, codec, seataOut, msgOut);
                            seataOut.writeInt((int)msgId);
                            Map<String, String> headMap = new HashMap<>();
                            headMap.put("protocal", "GtsToSeata");
                            int headMapBytesLength = HeadMapSerializer.getInstance().encode(headMap, seataOut);
                            headLength += headMapBytesLength;
                            fullLength += headMapBytesLength;

                            // serialize
                            seataOut.writeBytes(msgOut); // skip header map, direct writeBody

                            fullLength += msgOut.length;
                            // fix fullLength and headLength
                            int writeIndex = seataOut.writerIndex();
                            // skip magic code(2B) + version(1B)
                            seataOut.writerIndex(writeIndex - fullLength + 3);
                            seataOut.writeInt(fullLength);
                            seataOut.writeShort(headLength);
                            seataOut.writerIndex(writeIndex);
                            gtsRpcMessage.setBody(codec);
                            this.decode(ctx, seataOut);
                        } else {
                            byte[] body = new byte[bodyLength];
                            in.readBytes(body);
                            throw new TxcException("hessianDeserialize error");
                        }
                    } catch (Exception var20) {
                        LOGGER.error("Gts Decode error, cause: {}", var20.getMessage());
                        throw new DecodeException(var20);
                    }
                    if(LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Receive: " + gtsRpcMessage.getBody() + ",messageId: " + msgId);
                    }

                }
            }
        }
    }

    public Object changetoSeataCodec(short typeCode, TxcCodec gtsCodec, ByteBuf out, byte[] msgOut) {
        switch (typeCode) {
            case 1:
            {
                GlobalBeginRequest globalBeginRequest = new GlobalBeginRequest();
                BeginMessage beginMessage = (BeginMessage) gtsCodec;
                short timeout = (short) beginMessage.getTimeout();
                globalBeginRequest.setTimeout(timeout);
                String transactionName = beginMessage.getTxcInst();
                globalBeginRequest.setTransactionName(transactionName);
                // message type
                out.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
                // Serializer (default: seata)
                out.writeByte(2);
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(2));
                msgOut = serializer.serialize(globalBeginRequest);
                // Compress
                out.writeByte(0);
                return globalBeginRequest;
            }
            case 2:
            {
                GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
                BeginResultMessage beginResultMessage = (BeginResultMessage) gtsCodec;
                String xid = beginResultMessage.getXid();
                globalBeginResponse.setXid(xid);
                // message type
                out.writeByte(ProtocolConstants.MSGTYPE_RESPONSE);
                // Serializer (default: seata)
                out.writeByte(2);
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(2));
                msgOut = serializer.serialize(globalBeginResponse);
                // Compress
                out.writeByte(0);
                return globalBeginResponse;
            }
            case 3:
            {
                BranchCommitRequest branchCommitRequest = new BranchCommitRequest();
                BranchCommitMessage branchCommitMessage = (BranchCommitMessage) gtsCodec;
                String serverAddr = branchCommitMessage.getServerAddr();
                String xid = serverAddr + ":" + String.valueOf(branchCommitMessage.getTranIds().get(0));
                Long branchId = branchCommitMessage.getBranchIds().get(0);
                String resourceId = branchCommitMessage.getDbName();
                String applicationData = branchCommitMessage.getUdata();
                branchCommitRequest.setXid(xid);
                branchCommitRequest.setBranchId(branchId);
                branchCommitRequest.setResourceId(resourceId);
                branchCommitRequest.setApplicationData(applicationData);
                branchCommitRequest.setBranchType(BranchType.GTS);
                // message type
                out.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
                // Serializer (default: seata)
                out.writeByte(2);
                Serializer serializer = SerializerServiceLoader.load(SerializerType.getByCode(2));
                msgOut = serializer.serialize(branchCommitRequest);
                // Compress
                out.writeByte(0);
                return branchCommitRequest;
            }
            case 4:
            {

            }
        }
        return null;
    }

    public TxcCodec getTxcCodecInstance(short typeCode) {
        TxcCodec codec;
        switch (typeCode) {
            case 1:
                codec = new BeginMessage();
                break;
            case 2:
                codec = new BeginResultMessage();
                break;
            case 3:
                codec = new BranchCommitMessage();
                break;
            case 4:
                codec = new BranchCommitResultMessage();
                break;
            case 5:
                codec = new BranchRollbackMessage();
                break;
            case 6:
                codec = new BranchRollbackResultMessage();
                break;
            case 7:
                codec = new GlobalCommitMessage();
                break;
            case 8:
                codec = new GlobalCommitResultMessage();
                break;
            case 9:
                codec = new GlobalRollbackMessage();
                break;
            case 10:
                codec = new GlobalRollbackResultMessage();
                break;
            case 11:
                codec = new RegisterMessage();
                break;
            case 12:
                codec = new RegisterResultMessage();
                break;
            case 13:
                codec = new ReportStatusMessage();
                break;
            case 14:
                codec = new ReportStatusResultMessage();
                break;
            case 15:
                codec = new BeginRetryBranchMessage();
                break;
            case 16:
                codec = new BeginRetryBranchResultMessage();
                break;
            case 17:
                codec = new ReportUdataMessage();
                break;
            case 18:
                codec = new ReportUdataResultMessage();
                break;
            case 19:
                codec = new TxcMergeMessage();
                break;
            case 20:
                codec = new TxcMergeResultMessage();
                break;
            case 21:
                codec = new QueryLockMessage();
                break;
            case 22:
                codec = new QueryLockResultMessage();
                break;
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            default:
                String className = (String)TxcMessage.typeMap.get(typeCode);
                throw new TxcException("unknown class:" + className + " in txc message codec.");
            case 101:
                codec = new RegisterClientAppNameMessage();
                break;
            case 102:
                codec = new RegisterClientAppNameResultMessage();
                break;
            case 103:
                codec = new RegisterRmMessage();
                break;
            case 104:
                codec = new RegisterRmResultMessage();
                break;
            case 113:
                codec = new ClusterDumpMessage();
                break;
            case 114:
                codec = new ClusterDumpResultMessage();
                break;
            case 121:
                codec = new RedressMessage();
                break;
            case 122:
                codec = new RedressResultMessage();
        }

        return codec;
    }
}
