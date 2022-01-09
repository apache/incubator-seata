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
package io.seata.saga.engine.pcext.utils;

import io.seata.common.util.StringUtils;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;

/**
 * @author anselleeyy
 */
public class ParallelTaskUtils {

    public static final String PARALLEL_STATE_NAME_PATTERN = "%s-parallel-%d-%s";

    public static String generateParallelSubStateName(ProcessContext context, String stateName) {
        if (StringUtils.isNotBlank(stateName)) {
            String parentStateName = (String) context.getVariable(DomainConstants.PARALLEL_PARENT_STATE_NAME);
            int branchIndex = (int) context.getVariable(DomainConstants.PARALLEL_BRANCH_INDEX);
            return String.format(PARALLEL_STATE_NAME_PATTERN, parentStateName, branchIndex, stateName);
        }
        return stateName;
    }

    public static ProcessContext createTempContext(ProcessContext context, String currentBranchStateName, int branchIndex) {

        ProcessContextImpl tempContext = new ProcessContextImpl();
        tempContext.setParent(context);
        tempContext.setInstruction(copyInstruction(context.getInstruction(StateInstruction.class), currentBranchStateName));
        tempContext.setVariableLocally(DomainConstants.PARALLEL_BRANCH_INDEX, branchIndex);

        return tempContext;
    }

    public static StateInstruction copyInstruction(StateInstruction instruction, String stateName) {
        StateInstruction targetInstruction = new StateInstruction();
        targetInstruction.setStateMachineName(instruction.getStateMachineName());
        targetInstruction.setTenantId(instruction.getTenantId());
        targetInstruction.setStateName(stateName);
        targetInstruction.setTemporaryState(instruction.getTemporaryState());
        return targetInstruction;
    }

}
