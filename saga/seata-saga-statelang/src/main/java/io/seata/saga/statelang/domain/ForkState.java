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

package io.seata.saga.statelang.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fork state for multi-branch parallel execution
 *
 * @author ptyin
 */
public interface ForkState extends State {
    /**
     * Get branch list.
     *
     * @return branch list
     */
    List<String> getBranches();

    /**
     * Get parallelism, i.e. max thread count. The default is 0 stands for no limit.
     *
     * @return parallelism
     */
    int getParallel();

    /**
     * Get timeout. The default is 0, i.e. no timeout.
     *
     * @return timeout
     */
    long getTimeout();

    /**
     * Get paired join state name.
     *
     * @return name of paired join state
     */
    String getPairedJoinState();

    /**
     * Get all branch states map with branch initial state as key and branch states as value.
     * Note that values of the map do not include states inside branches of sub fork state.
     *
     * @return all branch states
     */
    Map<String, Set<String>> getAllBranchStates();
}
