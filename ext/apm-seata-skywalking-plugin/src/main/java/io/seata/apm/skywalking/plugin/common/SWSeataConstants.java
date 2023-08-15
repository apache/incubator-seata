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

import java.util.HashSet;
import java.util.Set;

/**
 * @author zhaoyuguang
 */
public class SWSeataConstants {

    private static final Set<String> TRANSACTION_MANAGER_OPERATION_NAME_MAPPING = new HashSet<>();

    static {
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalBeginRequest");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalBeginResponse");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalRollbackRequest");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalRollbackResponse");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalCommitRequest");
        TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.add("GlobalCommitResponse");
    }

    public static boolean isTransactionManagerOperationName(String operationName) {
        return TRANSACTION_MANAGER_OPERATION_NAME_MAPPING.contains(operationName);
    }
}
