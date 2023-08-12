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

import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.SideEffectFreeProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.impl.LoopStartStateImpl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Parallel task util
 *
 * @author ptyin
 */
public class ParallelTaskUtils {

    /**
     * Forked context based on parameter
     *
     * @param parentContext parent context
     * @return child context
     */
    public static ProcessContext forkProcessContext(ProcessContext parentContext, State branchState,
                                                    CountDownLatch latch, Semaphore semaphore) {
        SideEffectFreeProcessContextImpl childContext = new SideEffectFreeProcessContextImpl();
        childContext.setParent(parentContext);
        childContext.setVariable(DomainConstants.PARALLEL_LATCH, latch);
        childContext.setVariable(DomainConstants.PARALLEL_SEMAPHORE, semaphore);

        StateInstruction parentInstruction = parentContext.getInstruction(StateInstruction.class);
        StateInstruction copiedInstruction = copyInstruction(parentInstruction);
        copiedInstruction.setStateName(branchState.getName());
        if (LoopTaskUtils.getLoopConfig(parentContext, branchState) != null) {
            copiedInstruction.setTemporaryState(new LoopStartStateImpl());
        }
        childContext.setInstruction(copiedInstruction);

        return childContext;
    }

    public static StateInstruction copyInstruction(StateInstruction instruction) {
        StateInstruction copiedInstruction = new StateInstruction();
        copiedInstruction.setStateName(instruction.getStateName());
        copiedInstruction.setStateMachineName(instruction.getStateMachineName());
        copiedInstruction.setEnd(instruction.isEnd());
        copiedInstruction.setTemporaryState(instruction.getTemporaryState());
        copiedInstruction.setTenantId(instruction.getTenantId());
        return copiedInstruction;
    }

    public static void endBranch(ProcessContext context) {
        if (context.hasVariable(DomainConstants.PARALLEL_SEMAPHORE)) {
            Semaphore semaphore = (Semaphore) context.getVariable(DomainConstants.PARALLEL_SEMAPHORE);
            semaphore.release();
        }

        if (context.hasVariable(DomainConstants.PARALLEL_LATCH)) {
            CountDownLatch latch = (CountDownLatch) context.getVariable(DomainConstants.PARALLEL_LATCH);
            latch.countDown();
        }
    }


}
