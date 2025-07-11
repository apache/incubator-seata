package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.param.GlobalLockParam;
import org.apache.seata.mcp.service.GlobalLockService;
import org.apache.seata.mcp.service.MCPRPCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class GlobalLockServiceImpl implements GlobalLockService {
    @Autowired
    @Lazy
    private MCPRPCService mcpRPCService;

    private final String GLOBAL_LOCK_BASE_URL = "/api/v1/console/globalLock";

    @Override
    public String queryGlobalLock(GlobalLockParam param) {
        String result = mcpRPCService.getCallTC(GLOBAL_LOCK_BASE_URL+"/query"
                ,param,null,null);
        if(StringUtils.isBlank(result)){
            return "query global lock failed";
        }else{
            return result;
        }
    }

    @Override
    public String deleteGlobalLock(GlobalLockParam param) {
        String result = mcpRPCService.deleteCallTC(GLOBAL_LOCK_BASE_URL+"/delete"
                ,param,null,null);
        if(StringUtils.isBlank(result)){
            return "delete global lock failed";
        }else{
            return result;
        }
    }

    @Override
    public String checkGlobalLock(String xid, String branchId) {
        Map<String,String> pathParams = new HashMap<>();
        pathParams.put("xid",xid);
        pathParams.put("branchId",branchId);
        String result = mcpRPCService.getCallTC(GLOBAL_LOCK_BASE_URL+"/check"
                ,null,pathParams,null);
        if(StringUtils.isBlank(result)){
            return String.format("check global lock failed, xid: %s, branchId: %s", xid, branchId);
        }else{
            return result;
        }
    }
}
