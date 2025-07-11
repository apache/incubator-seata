package org.apache.seata.mcp.controller.tools;



import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.GlobalSessionParam;
import org.apache.seata.mcp.service.GlobalSessionService;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Use RPC to call the server-side corresponding method
 */
@Service
public class GlobalSessionTools {

    @Autowired
    private GlobalSessionService globalSessionService;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionTools.class);


    @Tool(description = "Query global transactions")
    public String queryGlobalSession(@ToolParam(description = "Query parameter objects") GlobalSessionParam param) {
        return globalSessionService.queryGlobalSession(param);
    }

    @Tool(description = "Delete the global session, Get the modify key before you delete")
    public String deleteGlobalSession(@ToolParam(description = "Global transaction id") String xid,@ToolParam(description = "Modify key") String modifyKey){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the global session, xid: {}", xid);
        }
        if(modifyConfirmService.isValidKey(modifyKey)){
            return globalSessionService.deleteGlobalSession(xid);
        }else{
            return "the modify key is not available";
        }
    }

    @Tool(description = "Force Delete the global session, Get the modify key before you delete")
    public String forceDeleteGlobalSession(@ToolParam(description = "Global transaction id") String xid,@ToolParam(description = "Modify key") String modifyKey){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to force delete the global session, xid: {}", xid);
        }
        if(modifyConfirmService.isValidKey(modifyKey)){
            return globalSessionService.forceDeleteGlobalSession(xid);
        }else{
            return "the modify key is not available";
        }
    }

    @Tool(description = "Stop the global session retry, Get the modify key before you stop")
    public String stopGlobalSession(@ToolParam(description = "Global transaction id") String xid,@ToolParam(description = "Modify key") String modifyKey){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to stop the global session, xid: {}", xid);
        }
        if(modifyConfirmService.isValidKey(modifyKey)){
            return globalSessionService.stopGlobalSession(xid);
        }else{
            return "the modify key is not available";
        }
    }

    @Tool(description = "Start the global session retry, Get the modify key before you start")
    public String startGlobalSession(@ToolParam(description = "Global transaction id") String xid,@ToolParam(description = "Modify key") String modifyKey){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to start the global session, xid: {}", xid);
        }
        if(modifyConfirmService.isValidKey(modifyKey)){
            return globalSessionService.startGlobalSession(xid);
        }else{
            return "the modify key is not available";
        }
    }

    @Tool(description = "Send global session to commit or rollback to rm, Get the modify key before you send")
    public String sendCommitOrRollback(@ToolParam(description = "Global transaction id") String xid,@ToolParam(description = "Modify key") String modifyKey){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to commit or rollback the global session, xid: {}", xid);
        }
        if(modifyConfirmService.isValidKey(modifyKey)){
            return globalSessionService.sendCommitOrRollback(xid);
        }else{
            return "the modify key is not available";
        }
    }

    @Tool(description = "Change the global session status, Get the modify key before you change")
    public String changeGlobalStatus(@ToolParam(description = "Global transaction id") String xid,@ToolParam(description = "Modify key") String modifyKey){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to change the global session, xid: {}", xid);
        }
        if(modifyConfirmService.isValidKey(modifyKey)){
            return globalSessionService.changeGlobalStatus(xid);
        }else{
            return "the modify key is not available";
        }
    }
}
