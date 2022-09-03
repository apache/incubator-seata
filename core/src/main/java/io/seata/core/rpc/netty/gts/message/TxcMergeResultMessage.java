package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import io.seata.core.rpc.netty.gts.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class TxcMergeResultMessage extends TxcMessage implements MergeMessage {
    private static final long serialVersionUID = -7719219648774528552L;
    public AbstractResultMessage[] msgs;
    private static final Logger LOGGER = LoggerFactory.getLogger(TxcMergeResultMessage.class);

    public TxcMergeResultMessage() {
    }

    public AbstractResultMessage[] getMsgs() {
        return this.msgs;
    }

    public void setMsgs(AbstractResultMessage[] msgs) {
        this.msgs = msgs;
    }

    @Override
    public short getTypeCode() {
        return 20;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(this.msgs.length * 1024);
        byteBuffer.putShort((short)this.msgs.length);
        AbstractResultMessage[] var2 = this.msgs;
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            TxcMessage msg = var2[var4];
            msg.setChannelHandlerContext(this.ctx);
            byte[] data = msg.encode();
            byteBuffer.putShort(msg.getTypeCode());
            byteBuffer.put(data);
        }

        byteBuffer.flip();
        int length = byteBuffer.limit();
        byte[] content = new byte[length + 4];
        intToBytes(length, content, 0);
        byteBuffer.get(content, 4, length);
        if (this.msgs.length > 20 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("msg in one txc merge packet:" + this.msgs.length + ",buffer size:" + content.length);
        }

        return content;
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 4) {
            return false;
        } else {
            i -= 4;
            int length = in.readInt();
            if (i < length) {
                return false;
            } else {
                byte[] buffer = new byte[length];
                in.readBytes(buffer);
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                this.decode(byteBuffer);
                return true;
            }
        }
    }

    public void decode(ByteBuffer byteBuffer) {
        short msgNum = byteBuffer.getShort();
        this.msgs = new AbstractResultMessage[msgNum];

        for(int idx = 0; idx < msgNum; ++idx) {
            short typeCode = byteBuffer.getShort();
            MergedMessage message = null;
            switch(typeCode) {
                case 2:
                    message = new BeginResultMessage();
                    break;
                case 3:
                case 5:
                case 7:
                case 9:
                case 11:
                case 13:
                case 15:
                case 17:
                case 19:
                case 20:
                case 21:
                default:
                    String className = (String) typeMap.get(typeCode);
                    throw new TxcException("unknown class:" + className + " in txc merge result message.", TxcErrCode.MergeResultMessageError);
                case 4:
                    message = new BranchCommitResultMessage();
                    break;
                case 6:
                    message = new BranchRollbackResultMessage();
                    break;
                case 8:
                    message = new GlobalCommitResultMessage();
                    break;
                case 10:
                    message = new GlobalRollbackResultMessage();
                    break;
                case 12:
                    message = new RegisterResultMessage();
                    break;
                case 14:
                    message = new ReportStatusResultMessage();
                    break;
                case 16:
                    message = new BeginRetryBranchResultMessage();
                    break;
                case 18:
                    message = new ReportUdataResultMessage();
                    break;
                case 22:
                    message = new QueryLockResultMessage();
            }

            ((TxcMessage)message).setChannelHandlerContext(this.ctx);
            ((MergedMessage)message).decode(byteBuffer);
            this.msgs[idx] = (AbstractResultMessage)message;
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TxcMergeResultMessage ");
        AbstractResultMessage[] var2 = this.msgs;
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            TxcMessage msg = var2[var4];
            sb.append(msg.toString()).append("\n");
        }

        return sb.toString();
    }

}
