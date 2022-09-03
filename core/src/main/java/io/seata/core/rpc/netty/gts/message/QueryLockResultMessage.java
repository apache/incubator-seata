package io.seata.core.rpc.netty.gts.message;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class QueryLockResultMessage extends AbstractResultMessage implements Serializable {
    private static final long serialVersionUID = 442197201232578329L;
    long tranId;
    String businessKey;

    public QueryLockResultMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public String getBusinessKey() {
        return this.businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String toString() {
        return "QueryLockResultMessage result:" + this.result + ",message:" + this.getMsg();
    }

    public short getTypeCode() {
        return 22;
    }

    public byte[] encode() {
        super.encode();
        this.byteBuffer.flip();
        byte[] content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
    }
}

