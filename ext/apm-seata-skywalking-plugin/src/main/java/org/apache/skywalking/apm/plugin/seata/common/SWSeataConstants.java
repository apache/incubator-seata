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
 *
 */
package org.apache.skywalking.apm.plugin.seata.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhaoyuguang
 */
public class SWSeataConstants {

    public static final Map<String, String> OPERATION_NAME_MAPPING = new HashMap<>(100);
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
    }
}
