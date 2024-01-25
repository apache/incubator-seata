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
package org.apache.seata.saga.engine.pcext.utils;

import java.util.Collection;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.DomainConstants;

/**
 * Loop Context Holder for Loop Attributes
 *
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
