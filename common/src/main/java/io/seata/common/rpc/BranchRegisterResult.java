package io.seata.common.rpc;

/**
 * Result of branch register.
 *
 * @author longchenming
 */
public class BranchRegisterResult {

    private long branchId;

    private int timeout;

    public long getBranchId() {
        return branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

}
