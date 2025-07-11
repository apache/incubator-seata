package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.GlobalLockParam;
import org.apache.seata.mcp.service.GlobalLockService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
@Service
public class GlobalLockTools {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLockTools.class.getName());

    @Autowired
    private GlobalLockService globalLockService;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    @Tool(description = "Query the global lock information")
    public String queryGlobalLock(@ToolParam(description = "Global lock parameters") GlobalLockParam param) {
        return globalLockService.queryGlobalLock(param);
    }

    @Tool(description = "Delete the global lock, Get the modify key before you delete")
    public String deleteGlobalLock(@ToolParam(description = "Global lock parameters") GlobalLockParam param,@ToolParam(description = "Modify key") String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the global lock, param: {}", param);
        }
        if(modifyConfirmService.isValidKey(modifyKey)){
            return globalLockService.deleteGlobalLock(param);
        }else{
            return "the modify key is not available";
        }
    }

    @Tool(description = "Check if the lock exist the branch session")
    public String checkGlobalLock(@ToolParam(description = "Global transaction id") String xid,@ToolParam(description = "Branch transaction id")String branchId) {
        return globalLockService.checkGlobalLock(xid, branchId);
    }
}
