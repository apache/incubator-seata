/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.apm.skywalking.plugin.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhaoyuguang
 */
public class SWSeataConstants {

    public static final Map<String, String> OPERATION_NAME_MAPPING = new HashMap<>();
    public static final String SEATA_NAME = "SEATA";

    static {
        OPERATION_NAME_MAPPING.put("GlobalBeginRequest", SEATA_NAME + "/TM/" + "GlobalBeginRequest");
        OPERATION_NAME_MAPPING.put("GlobalBeginResponse", SEATA_NAME + "/TM/" + "GlobalBeginResponse");
        OPERATION_NAME_MAPPING.put("GlobalRollbackRequest", SEATA_NAME + "/TM/" + "GlobalRollbackRequest");
        OPERATION_NAME_MAPPING.put("GlobalRollbackResponse", SEATA_NAME + "/TM/" + "GlobalRollbackResponse");
        OPERATION_NAME_MAPPING.put("GlobalCommitRequest", SEATA_NAME + "/TM/" + "GlobalCommitRequest");
        OPERATION_NAME_MAPPING.put("GlobalCommitResponse", SEATA_NAME + "/TM/" + "GlobalCommitResponse");
        OPERATION_NAME_MAPPING.put("BranchRegisterRequest", SEATA_NAME + "/RM/" + "BranchRegisterRequest");
        OPERATION_NAME_MAPPING.put("BranchRegisterResponse", SEATA_NAME + "/RM/" + "BranchRegisterResponse");
        OPERATION_NAME_MAPPING.put("BranchRollbackRequest", SEATA_NAME + "/RM/" + "BranchRollbackRequest");
        OPERATION_NAME_MAPPING.put("BranchRollbackResponse", SEATA_NAME + "/RM/" + "BranchRollbackResponse");
        OPERATION_NAME_MAPPING.put("BranchCommitRequest", SEATA_NAME + "/RM/" + "BranchCommitRequest");
        OPERATION_NAME_MAPPING.put("BranchCommitResponse", SEATA_NAME + "/RM/" + "BranchCommitResponse");
        OPERATION_NAME_MAPPING.put("BranchReportRequest", SEATA_NAME + "/RM/" + "BranchReportRequest");
        OPERATION_NAME_MAPPING.put("BranchReportResponse", SEATA_NAME + "/RM/" + "BranchReportResponse");
        OPERATION_NAME_MAPPING.put("GlobalLockQueryRequest", SEATA_NAME + "/RM/" + "GlobalLockQueryRequest");
        OPERATION_NAME_MAPPING.put("GlobalLockQueryResponse", SEATA_NAME + "/RM/" + "GlobalLockQueryResponse");
        OPERATION_NAME_MAPPING.put("UndoLogDeleteRequest", SEATA_NAME + "/RM/" + "UndoLogDeleteRequest");
        OPERATION_NAME_MAPPING.put("UndoLogDeleteResponse", SEATA_NAME + "/RM/" + "UndoLogDeleteResponse");
        OPERATION_NAME_MAPPING.put("RegisterRMRequest", SEATA_NAME + "/RM/" + "RegisterRMRequest");
        OPERATION_NAME_MAPPING.put("RegisterRMResponse", SEATA_NAME + "/RM/" + "RegisterRMResponse");
        OPERATION_NAME_MAPPING.put("RegisterTMRequest", SEATA_NAME + "/RM/" + "RegisterTMRequest");
        OPERATION_NAME_MAPPING.put("RegisterTMResponse", SEATA_NAME + "/RM/" + "RegisterTMResponse");
    }
}
