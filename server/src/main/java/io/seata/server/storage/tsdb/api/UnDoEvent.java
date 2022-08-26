package io.seata.server.storage.tsdb.api;

public class UnDoEvent {
    public String xid;
    public long branchId;
    public String rollbackCtx;
    public byte[] undoLogContent;

    public UnDoEvent(String xid, long branchId, String rollbackCtx, byte[] undoLogContent) {
        this.xid = xid;
        this.branchId = branchId;
        this.rollbackCtx = rollbackCtx;
        this.undoLogContent = undoLogContent;
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

    public String getRollbackCtx() {
        return rollbackCtx;
    }

    public void setRollbackCtx(String rollbackCtx) {
        this.rollbackCtx = rollbackCtx;
    }

    public byte[] getUndoLogContent() {
        return undoLogContent;
    }

    public void setUndoLogContent(byte[] undoLogContent) {
        this.undoLogContent = undoLogContent;
    }
}
