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

import io.seata.common.util.CollectionUtils;
import io.seata.saga.statelang.domain.ForkState;
import io.seata.saga.statelang.domain.impl.ForkStateImpl;
import io.seata.saga.statelang.parser.ParserException;
import io.seata.saga.statelang.parser.StateParser;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Fork state parser
 *
 * @author ptyin
 */
public class ForkStateParser extends BaseStatePaser implements StateParser<ForkState> {
    @Override
    @SuppressWarnings("unchecked")
    public ForkState parse(Object node) {
        ForkStateImpl forkState = new ForkStateImpl();
        parseBaseAttributes(forkState, node);

        Map<String, Object> nodeMap = (Map<String, Object>) node;
        List<String> branches = (List<String>) nodeMap.get("Branches");
        if (CollectionUtils.isEmpty(branches)) {
            throw new ParserException("Branches of fork state should not be empty");
        }
        if (new HashSet<>(branches).size() < branches.size()) {
            throw new ParserException("Branches of fork state should not be same.");
        }
        forkState.setBranches(branches);
        Integer parallel = (Integer) nodeMap.get("Parallel");
        if (parallel != null) {
            forkState.setParallel(parallel);
        }
        Integer awaitTimeout = (Integer) nodeMap.get("AwaitTimeout");
        if (awaitTimeout != null) {
            forkState.setAwaitTimeout(awaitTimeout);
        }

        return forkState;
    }
}
