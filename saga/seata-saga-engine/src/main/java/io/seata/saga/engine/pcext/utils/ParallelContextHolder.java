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

import java.util.ArrayList;
import java.util.List;

import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;

/**
 * Parallel Context Holder for parallel state
 *
 * @author anselleeyy
 */
public class ParallelContextHolder {

    private          List<String> initBranches     = new ArrayList<>();
    private          List<String> forwardBranches  = new ArrayList<>();
    private          List<String> executedBranches = new ArrayList<>();
    private volatile boolean      failEnd          = false;

    public static ParallelContextHolder getCurrent(ProcessContext context, boolean forceCreate) {

        ParallelContextHolder parallelContextHolder =
            (ParallelContextHolder) context.getVariable(DomainConstants.VAR_NAME_CURRENT_PARALLEL_CONTEXT_HOLDER);
        if (null == parallelContextHolder && forceCreate) {
            synchronized (context) {
                parallelContextHolder =
                    (ParallelContextHolder) context.getVariable(DomainConstants.VAR_NAME_CURRENT_PARALLEL_CONTEXT_HOLDER);
                if (null == parallelContextHolder) {
                    parallelContextHolder = new ParallelContextHolder();
                    context.setVariable(DomainConstants.VAR_NAME_CURRENT_PARALLEL_CONTEXT_HOLDER,
                        parallelContextHolder);
                }
            }
        }
        return parallelContextHolder;
    }

    public static void clearCurrent(ProcessContext context) {
        context.removeVariable(DomainConstants.VAR_NAME_CURRENT_PARALLEL_CONTEXT_HOLDER);
    }

    /**
     * Getter method for property <tt>startBranches</tt>.
     *
     * @return property value of startBranches
     */
    public List<String> getInitBranches() {
        return initBranches;
    }

    /**
     * Setter method for property <tt>startBranches</tt>.
     *
     * @param initBranches value to be assigned to property startBranches
     */
    public void setInitBranches(List<String> initBranches) {
        this.initBranches = initBranches;
    }

    /**
     * Getter method for property <tt>forwardBranches</tt>.
     *
     * @return property value of forwardBranches
     */
    public List<String> getForwardBranches() {
        return forwardBranches;
    }

    /**
     * Setter method for property <tt>forwardBranches</tt>.
     *
     * @param forwardBranches value to be assigned to property forwardBranches
     */
    public void setForwardBranches(List<String> forwardBranches) {
        this.forwardBranches = forwardBranches;
    }

    /**
     * Getter method for property <tt>executedBranches</tt>.
     *
     * @return property value of executedBranches
     */
    public List<String> getExecutedBranches() {
        return executedBranches;
    }

    /**
     * Setter method for property <tt>executedBranches</tt>.
     *
     * @param executedBranches value to be assigned to property executedBranches
     */
    public void setExecutedBranches(List<String> executedBranches) {
        this.executedBranches = executedBranches;
    }

    /**
     * Getter method for property <tt>failEnd</tt>.
     *
     * @return property value of failEnd
     */
    public boolean isFailEnd() {
        return failEnd;
    }

    /**
     * Setter method for property <tt>failEnd</tt>.
     *
     * @param failEnd value to be assigned to property failEnd
     */
    public void setFailEnd(boolean failEnd) {
        this.failEnd = failEnd;
    }
}
