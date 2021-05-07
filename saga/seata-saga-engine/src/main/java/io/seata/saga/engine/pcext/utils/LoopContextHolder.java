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

import java.util.Collection;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;

/**
 * Loop Context Holder for Loop Attributes
 *
 * @author anselleeyy
 */
public class LoopContextHolder {

    private final AtomicInteger nrOfInstances = new AtomicInteger();
    private final AtomicInteger nrOfActiveInstances = new AtomicInteger();
    private final AtomicInteger nrOfCompletedInstances = new AtomicInteger();
    private volatile boolean failEnd = false;
    private volatile boolean completionConditionSatisfied = false;
    private final Stack<Integer> loopCounterStack = new Stack<>();
    private final Stack<Integer> forwardCounterStack = new Stack<>();
    private Collection collection;

    public static LoopContextHolder getCurrent(ProcessContext context, boolean forceCreate) {
        LoopContextHolder loopContextHolder = (LoopContextHolder)context.getVariable(
            DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER);

        if (null == loopContextHolder && forceCreate) {
            synchronized (context) {
                loopContextHolder = (LoopContextHolder)context.getVariable(
                    DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER);
                if (null == loopContextHolder) {
                    loopContextHolder = new LoopContextHolder();
                    context.setVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER, loopContextHolder);
                }
            }
        }
        return loopContextHolder;
    }

    public static void clearCurrent(ProcessContext context) {
        context.removeVariable(DomainConstants.VAR_NAME_CURRENT_LOOP_CONTEXT_HOLDER);
    }

    public AtomicInteger getNrOfInstances() {
        return nrOfInstances;
    }

    public AtomicInteger getNrOfActiveInstances() {
        return nrOfActiveInstances;
    }

    public AtomicInteger getNrOfCompletedInstances() {
        return nrOfCompletedInstances;
    }

    public boolean isFailEnd() {
        return failEnd;
    }

    public void setFailEnd(boolean failEnd) {
        this.failEnd = failEnd;
    }

    public boolean isCompletionConditionSatisfied() {
        return completionConditionSatisfied;
    }

    public void setCompletionConditionSatisfied(boolean completionConditionSatisfied) {
        this.completionConditionSatisfied = completionConditionSatisfied;
    }

    public Stack<Integer> getLoopCounterStack() {
        return loopCounterStack;
    }

    public Stack<Integer> getForwardCounterStack() {
        return forwardCounterStack;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }
}
