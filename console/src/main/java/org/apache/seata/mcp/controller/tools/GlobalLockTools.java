/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.GlobalLockDeleteParam;
import org.apache.seata.mcp.entity.param.GlobalLockParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.GlobalLockService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GlobalLockTools {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalLockTools.class.getName());

    @Autowired
    private GlobalLockService globalLockService;

    @Autowired
    private ModifyConfirmService modifyConfirmService;

    @Tool(description = "Query the global lock information")
    public String queryGlobalLock(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global lock parameters", required = true) GlobalLockParam param) {
        return globalLockService.queryGlobalLock(nameSpaceDetail, param);
    }

    @Tool(description = "Delete the global lock, Get the modify key before you delete")
    public String deleteGlobalLock(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global lock delete parameters", required = true) GlobalLockDeleteParam param,
            @ToolParam(description = "Modify key", required = true) String modifyKey) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("manual operation to delete the global lock, param: {}", param);
        }
        if (modifyConfirmService.isValidKey(modifyKey)) {
            return globalLockService.deleteGlobalLock(nameSpaceDetail, param);
        } else {
            return "the modify key is not available";
        }
    }

    @Tool(description = "Check if the lock exist the branch session")
    public String checkGlobalLock(
            @ToolParam(description = "Specify the namespace of the TC node", required = true)
                    NameSpaceDetail nameSpaceDetail,
            @ToolParam(description = "Global transaction id", required = true) String xid,
            @ToolParam(description = "Branch transaction id", required = true) String branchId) {
        return globalLockService.checkGlobalLock(nameSpaceDetail, xid, branchId);
    }
}
