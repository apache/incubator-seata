package io.seata.core.protocol.transaction;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.RpcContext;

import java.io.Serializable;

public class BranchDeleteRequest extends AbstractTransactionRequestToRM implements Serializable {

    private static final long serialVersionUID = 7134732523612364742L;

    /**
     * The Xid.
     */
    protected String xid;

    /**
     * The Branch id.
     */
    protected long branchId;

    /**
     * The Branch type.
     */
    protected BranchType branchType = BranchType.AT;

    /**
     * The resource id.
     */
    private String resourceId;

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_BRANCH_DELETE;
    }

    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        return handler.handle(this);
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public long getBranchId() {
        return branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }
}
