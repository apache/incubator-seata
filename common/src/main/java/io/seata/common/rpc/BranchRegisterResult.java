package io.seata.common.rpc;

/**
 * @author longchenming
 * @date 2021/12/20 16:10
 * @desc
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
