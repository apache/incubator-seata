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
package io.seata.saga.statelang.domain.impl;

import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ParallelState;

import java.util.List;

/**
 * Parallel State
 *
 * @author anselleeyy
 */
public class ParallelStateImpl extends AbstractTaskState implements ParallelState {

    private List<String> branches;
    private int parallel;

    public ParallelStateImpl() {
        setType(DomainConstants.STATE_TYPE_PARALLEL);
    }

    /**
     * Getter method for property <tt>branches</tt>.
     *
     * @return property value of branches
     */
    @Override
    public List<String> getBranches() {
        return branches;
    }

    /**
     * Setter method for property <tt>branches</tt>.
     *
     * @param branches value to be assigned to property branches
     */
    public void setBranches(List<String> branches) {
        this.branches = branches;
    }

    /**
     * Getter method for property <tt>parallel</tt>.
     *
     * @return property value of parallel
     */
    @Override
    public int getParallel() {
        return parallel;
    }

    /**
     * Setter method for property <tt>parallel</tt>.
     *
     * @param parallel value to be assigned to property parallel
     */
    public void setParallel(int parallel) {
        this.parallel = parallel;
    }
}