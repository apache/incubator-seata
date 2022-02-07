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
package io.seata.saga.statelang.parser.impl;

import java.util.List;
import java.util.Map;

import io.seata.common.util.CollectionUtils;
import io.seata.saga.statelang.domain.ParallelState;
import io.seata.saga.statelang.domain.impl.ParallelStateImpl;
import io.seata.saga.statelang.parser.StateParser;

/**
 * Parallel State Parser
 *
 * @author anselleeyy
 */
public class ParallelTaskStateParser extends AbstractTaskStateParser implements StateParser<ParallelState> {

    @Override
    public ParallelState parse(Object node) {

        ParallelStateImpl parallelState = new ParallelStateImpl();
        parseTaskAttributes(parallelState, node);

        Map<String, Object> nodeMap = (Map<String, Object>) node;

        // get parallel branches
        List<String> branches = (List<String>) nodeMap.get("Branches");
        if (CollectionUtils.isNotEmpty(branches)) {
            parallelState.setBranches(branches);
        }

        // parallel runtime threads
        Object parallel = nodeMap.get("Parallel");
        int threads = 1;
        // Compatible with String and Integer type
        if (parallel instanceof String) {
            threads = Integer.parseInt((String) parallel);
        } else if (parallel instanceof Integer) {
            threads = (Integer) parallel;
        }
        parallelState.setParallel(threads > 0 ? threads : 1);

        // parallel state should not be configured with loop attribution
        parallelState.setLoop(null);

        return parallelState;
    }

}
