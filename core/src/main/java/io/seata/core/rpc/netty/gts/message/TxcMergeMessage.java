package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import io.seata.core.rpc.netty.gts.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TxcMergeMessage extends TxcMessage implements Serializable, MergeMessage {
    private static final long serialVersionUID = -5758802337446717090L;
    public List<TxcMessage> msgs = new ArrayList();
    public List<Long> msgIds = new ArrayList();
    private static final Logger LOGGER = LoggerFactory.getLogger(TxcMergeMessage.class);

    public TxcMergeMessage() {
    }

    public short getTypeCode() {
        return 19;
    }

    public byte[] encode() {
        int bufferSize = this.msgs.size() * 1024;
        Iterator var2 = this.msgs.iterator();

        while(var2.hasNext()) {
            TxcMessage msg = (TxcMessage)var2.next();
            if (msg instanceof RegisterMessage) {
                String key = ((RegisterMessage)msg).getBusinessKey();
                if (key != null && key.length() > 512) {
                    int i = key.getBytes(UTF8).length;
                    LOGGER.info("get one huge registermessage, businesskey bytes:" + i);
                    bufferSize += i;
                }
            }
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
        byteBuffer.putShort((short)this.msgs.size());
        Iterator var7 = this.msgs.iterator();

        while(var7.hasNext()) {
            TxcMessage msg = (TxcMessage)var7.next();
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
        if (this.msgs.size() > 20 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("msg in one packet:" + this.msgs.size() + ",buffer size:" + content.length);
        }

        return content;
    }

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

        for(int idx = 0; idx < msgNum; ++idx) {
            short typeCode = byteBuffer.getShort();
            MergedMessage message = null;
            switch(typeCode) {
                case 1:
                    message = new BeginMessage();
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 8:
                case 10:
                case 12:
                case 14:
                case 16:
                case 18:
                case 19:
                case 20:
                default:
                    String className = (String) typeMap.get(typeCode);
                    throw new TxcException("unknown class:" + className + " in txc merge message.", TxcErrCode.MergeMessageError);
                case 7:
                    message = new GlobalCommitMessage();
                    break;
                case 9:
                    message = new GlobalRollbackMessage();
                    break;
                case 11:
                    message = new RegisterMessage();
                    break;
                case 13:
                    message = new ReportStatusMessage();
                    break;
                case 15:
                    message = new BeginRetryBranchMessage();
                    break;
                case 17:
                    message = new ReportUdataMessage();
                    break;
                case 21:
                    message = new QueryLockMessage();
            }

            ((TxcMessage)message).setChannelHandlerContext(this.ctx);
            ((MergedMessage)message).decode(byteBuffer);
            this.msgs.add((TxcMessage)message);
        }

    }

    public String toString() {
        StringBuilder sb = new StringBuilder("TxcMergeMessage ");
        Iterator var2 = this.msgs.iterator();

        while(var2.hasNext()) {
            TxcMessage msg = (TxcMessage)var2.next();
            sb.append(msg.toString()).append("\n");
        }

        return sb.toString();
    }

    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

}
