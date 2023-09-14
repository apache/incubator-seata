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
import io.seata.saga.statelang.domain.ForkState;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fork state implementation
 *
 * @author ptyin
 */
public class ForkStateImpl extends BaseState implements ForkState {
    private List<String> branches;

    // Default max parallel thread count is 0 which stands for no limits.
    private int parallel = 0;

    // Default timeout
    private int awaitTimeout = DomainConstants.DEFAULT_FORK_AWAIT_TIME;

    private String pairedJoinState;

    private Map<String, Set<String>> allBranchStates;

    public ForkStateImpl() {
        setType(DomainConstants.STATE_TYPE_FORK);
    }

    @Override
    public List<String> getBranches() {
        return branches;
    }

    public void setBranches(List<String> branches) {
        this.branches = branches;
    }

    @Override
    public int getParallel() {
        return parallel;
    }

    public void setParallel(int parallel) {
        this.parallel = parallel;
    }

    @Override
    public int getAwaitTimeout() {
        return awaitTimeout;
    }

    public void setAwaitTimeout(int awaitTimeout) {
        this.awaitTimeout = awaitTimeout;
    }

    @Override
    public String getPairedJoinState() {
        return pairedJoinState;
    }

    public void setPairedJoinState(String pairedJoinState) {
        this.pairedJoinState = pairedJoinState;
    }

    @Override
    public Map<String, Set<String>> getAllBranchStates() {
        return allBranchStates;
    }

    public void setAllBranchStates(Map<String, Set<String>> allBranchStates) {
        this.allBranchStates = allBranchStates;
    }
}
