package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BranchCommitResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = 1435821064508123741L;
    List<Long> tranIds;
    List<Long> branchIds;

    public BranchCommitResultMessage() {
        this(1024);
    }

    public BranchCommitResultMessage(int size) {
        this.tranIds = new ArrayList();
        this.branchIds = new ArrayList();
        this.byteBuffer = ByteBuffer.allocate(size);
    }

    public List<Long> getTranIds() {
        return this.tranIds;
    }

    public void setTranIds(List<Long> tranIds) {
        this.tranIds = tranIds;
    }

    public List<Long> getBranchIds() {
        return this.branchIds;
    }

    public void setBranchIds(List<Long> branchIds) {
        this.branchIds = branchIds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BranchCommitResultMessage ");
        if (this.branchIds.size() > 0) {
            sb.append(this.branchIds.get(0)).append("...");
        }

        sb.append(" result:").append(this.result).toString();
        return sb.toString();
    }

    @Override
    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    @Override
    public short getTypeCode() {
        return 4;
    }

    @Override
    public byte[] encode() {
        int i;
        if (this.tranIds.size() > 32) {
            i = this.tranIds.size();
            i = i / 64 + 2;
            this.byteBuffer = ByteBuffer.allocate(i * 1024);
        }

        super.encode();
        this.byteBuffer.putInt(this.tranIds.size());

        for(i = 0; i < this.tranIds.size(); ++i) {
            this.byteBuffer.putLong((Long)this.tranIds.get(i));
            this.byteBuffer.putLong((Long)this.branchIds.get(i));
        }

        this.byteBuffer.flip();
        byte[] content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    @Override
    public boolean decode(ByteBuf in) {
        if (!super.decode(in)) {
            return false;
        } else {
            int i = in.readableBytes();
            if (i < 4) {
                return false;
            } else {
                i -= 4;
                int size = in.readInt();
                if (i < 16 * size) {
                    return false;
                } else {
                    int var10000 = i - 16 * size;

                    for(int idx = 0; idx < size; ++idx) {
                        this.tranIds.add(in.readLong());
                        this.branchIds.add(in.readLong());
                    }

                    return true;
                }
            }
        }
    }
}

