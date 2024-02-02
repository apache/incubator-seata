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
package org.apache.seata.apm.skywalking.plugin.common;

import org.apache.seata.core.protocol.transaction.AbstractBranchEndRequest;
import org.apache.seata.core.protocol.transaction.AbstractBranchEndResponse;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.AbstractGlobalEndRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class SWSeataConstants {

    private static final Set<String> TRANSACTION_MANAGER_OPERATION_NAME_MAPPING = new HashSet<>();

    public static final HashMap<String,Class> TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING = new HashMap<>();

    static {
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalBeginRequest");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalBeginResponse");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalRollbackRequest");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalRollbackResponse");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalCommitRequest");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalCommitResponse");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalReportRequest");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalReportResponse");


        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("BranchCommitRequest", AbstractBranchEndRequest.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("BranchCommitResponse", AbstractBranchEndResponse.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("BranchRegisterRequest", BranchRegisterRequest.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("BranchRollbackRequest", AbstractBranchEndRequest.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("BranchRollbackResponse", AbstractBranchEndResponse.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("GlobalBeginResponse", GlobalBeginResponse.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("GlobalCommitRequest", AbstractGlobalEndRequest.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("GlobalRollbackRequest", AbstractGlobalEndRequest.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("GlobalStatusRequest", AbstractGlobalEndRequest.class);
        TRANSACTION_TRANSMISSION_CLASS_NAME_MAPPING.put("GlobalReportResponse", AbstractGlobalEndRequest.class);
    }

    public static boolean isTransactionManagerOperationName(String operationName) {
        return TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.contains(operationName);
    }
}
