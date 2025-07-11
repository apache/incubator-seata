package org.apache.seata.mcp.service;

public interface BranchSessionService {

    String deleteBranchSession(String xid,String branchId);

    String forceDeleteBranchSession(String xid,String branchId);

    String stopBranchSession(String xid,String branchId);

    String startBranchRetry(String xid,String branchId);
}
