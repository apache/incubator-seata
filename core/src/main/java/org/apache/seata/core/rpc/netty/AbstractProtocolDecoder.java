package org.apache.seata.core.rpc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.seata.core.exception.DecodeException;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProtocolDecoder extends LengthFieldBasedFrameDecoder implements ProtocolDecoder {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	public AbstractProtocolDecoder() {
		        /*
        int maxFrameLength,
        int lengthFieldOffset,  magic code is 2B, and version is 1B, and then FullLength. so value is 3
        int lengthFieldLength,  FullLength is int(4B). so values is 4
        int lengthAdjustment,   FullLength include all data and read 7 bytes before, so the left length is (FullLength-7). so values is -7
        int initialBytesToStrip we will check magic code and version self, so do not strip any bytes. so values is 0
        */
		super(ProtocolConstants.MAX_FRAME_LENGTH, 3, 4);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		Object decoded;
		try {
			decoded = super.decode(ctx, in);
			if (decoded instanceof ByteBuf) {
				ByteBuf frame = (ByteBuf)decoded;
				try {
					return decodeFrame(frame);
				} finally {
					frame.release();
				}
			}
		} catch (Exception exx) {
			logger.error("Decode frame error, cause: {}", exx.getMessage());
			throw new DecodeException(exx);
		}
		return decoded;
	}


}
