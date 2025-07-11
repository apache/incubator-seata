package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.controller.tools.GlobalSessionTools;
import org.apache.seata.mcp.entity.param.GlobalSessionParam;
import org.apache.seata.mcp.service.GlobalSessionService;
import org.apache.seata.mcp.service.MCPRPCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GlobalSessionServiceImpl implements GlobalSessionService {

    /**
     * Global transaction requests URL
     */
    private final String GLOBAL_SESSION_BASE_URL = "/api/v1/console/globalSession";

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSessionServiceImpl.class);

    @Autowired
    @Lazy
    private MCPRPCService mcpRPCService;


    @Override
    public String queryGlobalSession(GlobalSessionParam param) {
        String result = mcpRPCService.getCallTC(GLOBAL_SESSION_BASE_URL+"/query"
                ,param,null,null);
        if(StringUtils.isBlank(result)){
            return "query global session failed";
        }else{
            return result;
        }
    }

    @Override
    public String deleteGlobalSession(String xid) {
        Map<String,String> pathParams = new HashMap<>();
        pathParams.put("xid",xid);
        String result = mcpRPCService.deleteCallTC(GLOBAL_SESSION_BASE_URL+"/deleteGlobalSession"
                ,null,pathParams,null);
        if(StringUtils.isBlank(result)){
            return String.format("delete global session failed, xid: %s", xid);
        }else{
            return result;
        }
    }

    @Override
    public String forceDeleteGlobalSession(String xid) {
        Map<String,String> pathParams = new HashMap<>();
        pathParams.put("xid",xid);
        String result = mcpRPCService.deleteCallTC(GLOBAL_SESSION_BASE_URL+"/forceDeleteGlobalSession"
                ,null,pathParams,null);
        if(StringUtils.isBlank(result)){
            return String.format("force delete global session failed, xid: %s",xid);
        }else{
            return result;
        }
    }

    @Override
    public String stopGlobalSession(String xid) {
        Map<String,String> pathParams = new HashMap<>();
        pathParams.put("xid",xid);
        String result = mcpRPCService.putCallTC(GLOBAL_SESSION_BASE_URL+"/stopGlobalSession"
                , null,pathParams,null);
        if(StringUtils.isBlank(result)){
            return String.format("stop global session retry failed, xid: %s",xid);
        }else{
            return result;
        }
    }

    @Override
    public String startGlobalSession(String xid) {
        Map<String,String> pathParams = new HashMap<>();
        pathParams.put("xid",xid);
        String result = mcpRPCService.putCallTC(GLOBAL_SESSION_BASE_URL+"/startGlobalSession",
                null,pathParams,null);
        if(StringUtils.isBlank(result)){
            return String.format("start the global session retry failed, xid: %s",xid);
        }else {
            return result;
        }
    }

    @Override
    public String sendCommitOrRollback(String xid) {
        Map<String,String> pathParams = new HashMap<>();
        pathParams.put("xid",xid);
        String result = mcpRPCService.putCallTC(GLOBAL_SESSION_BASE_URL+"/sendCommitOrRollback"
                , null,pathParams,null);
        if(StringUtils.isBlank(result)){
            return String.format("send global session to commit or rollback to rm failed, xid: %s",xid);
        }else {
            return result;
        }
    }

    @Override
    public String changeGlobalStatus(String xid) {
        Map<String,String> pathParams = new HashMap<>();
        pathParams.put("xid",xid);
        String result = mcpRPCService.putCallTC(GLOBAL_SESSION_BASE_URL+"/changeGlobalStatus"
                , null,pathParams,null);
        if(StringUtils.isBlank(result)){
            return String.format("change the global session status failed, xid: %s",xid);
        }else {
            return result;
        }
    }
}
