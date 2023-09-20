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

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ForkState;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateMachineInstance;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Parallel context holder for parallel execution
 *
 * @author ptyin
 */
public class ParallelContextHolder {
    private List<String> branchNextList;
    private String joinNext;
    private AtomicInteger startedIndex;
    private AtomicInteger completedCount;
    private volatile boolean finished = false;
    private final AtomicBoolean alreadyGenerateJoinNext = new AtomicBoolean(false);
    private ParallelContextHolder parent;
    private long startTime;
    private long timeout;

    public static ParallelContextHolder getCurrent(ProcessContext context) {
        return (ParallelContextHolder) context.getVariable(DomainConstants.VAR_NAME_CURRENT_PARALLEL_CONTEXT_HOLDER);
    }

    public static ParallelContextHolder getInstance(ProcessContext context, ForkState forkState) {
        StateMachine stateMachine = forkState.getStateMachine();
        List<String> branchNextList;
        if (DomainConstants.OPERATION_NAME_FORWARD
                .equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))) {
            StateMachineInstance stateMachineInstance = (StateMachineInstance) context.getVariable(
                    DomainConstants.VAR_NAME_STATEMACHINE_INST);
            branchNextList = forkState.getBranches().stream()
                    .map(stateMachine::getState)
                    .map(branchState -> ParallelTaskUtils.forwardBranch(branchState, stateMachineInstance, forkState))
                    .map(State::getName)
                    .collect(Collectors.toList());
        } else {
            branchNextList = forkState.getBranches();
        }

        String pairedJoinStateName = forkState.getPairedJoinState();
        State pairedJoinState = forkState.getStateMachine().getState(pairedJoinStateName);
        if (pairedJoinState == null) {
            throw new EngineExecutionException(String.format("No paired join state [%s] found for fork state",
                    forkState.getName()), FrameworkErrorCode.ObjectNotExists);
        }

        ParallelContextHolder parallelContextHolder = new ParallelContextHolder();
        parallelContextHolder.branchNextList = Collections.unmodifiableList(branchNextList);
        parallelContextHolder.joinNext = pairedJoinState.getNext();
        parallelContextHolder.startedIndex = new AtomicInteger(0);
        parallelContextHolder.completedCount = new AtomicInteger(0);
        parallelContextHolder.parent = (ParallelContextHolder) context.getVariable(
                DomainConstants.VAR_NAME_CURRENT_PARALLEL_CONTEXT_HOLDER);
        parallelContextHolder.startTime = System.currentTimeMillis();
        parallelContextHolder.timeout = forkState.getTimeout();
        return parallelContextHolder;
    }

    public String next() {
        int startedIndexValue = startedIndex.getAndIncrement();
        if (startedIndexValue >= branchNextList.size()) {
            if (finished) {
                // Only generate join next once
                if (alreadyGenerateJoinNext.compareAndSet(false, true)) {
                    return joinNext;
                }
            }
            return null;
        } else {
            return branchNextList.get(startedIndexValue);
        }
    }

    public void complete() {
        int completedCountValue = completedCount.incrementAndGet();
        finished = completedCountValue == branchNextList.size();
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isTimeout() {
        if (timeout > 0 && System.currentTimeMillis() - startTime > timeout) {
            return true;
        }
        if (parent != null) {
            return parent.isTimeout();
        }
        return false;
    }
}
