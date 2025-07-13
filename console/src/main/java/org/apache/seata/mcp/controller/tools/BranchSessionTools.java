package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.service.BranchSessionService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BranchSessionTools {

    @Autowired
    private BranchSessionService branchSessionService;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    private static final Logger LOGGER = LoggerFactory.getLogger(BranchSessionTools.class);

    @Tool(description = "Delete branch transactions, Get the modify key before you delete")
    public String deleteBranchSession(
            @ToolParam(description = "Global transaction id") String xid,
            @ToolParam(description = "Branch transaction id") String branchId,
            @ToolParam(description = "Modify key") String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the branch session, xid: {} branchId: {}", xid, branchId);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return branchSessionService.deleteBranchSession(xid, branchId);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Force the deletion of branch transactions, Get the modify key before you delete")
    public String forceDeleteBranchSession(
            @ToolParam(description = "Global transaction id") String xid,
            @ToolParam(description = "Branch transaction id") String branchId,
            @ToolParam(description = "Modify key") String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to force delete the branch session, xid: {} branchId: {}", xid, branchId);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return branchSessionService.forceDeleteBranchSession(xid, branchId);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Stop the branch transaction retry, Get the modify key before you stop")
    public String stopBranchSession(
            @ToolParam(description = "Global transaction id") String xid,
            @ToolParam(description = "Branch transaction id") String branchId,
            @ToolParam(description = "Modify key") String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to stop the branch session, xid: {} branchId: {}", xid, branchId);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return branchSessionService.stopBranchSession(xid, branchId);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Initiate a branch transaction retries, Get the modify key before you start")
    public String startBranchRetry(
            @ToolParam(description = "Global transaction id") String xid,
            @ToolParam(description = "Branch transaction id") String branchId,
            @ToolParam(description = "Modify key") String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to start the branch session, xid: {} branchId: {}", xid, branchId);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return branchSessionService.startBranchRetry(xid, branchId);
        } else {
            return "the modify key is not available";
        }
    }
}
