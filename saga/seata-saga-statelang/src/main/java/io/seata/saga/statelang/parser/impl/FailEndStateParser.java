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

import java.util.Map;

import io.seata.saga.statelang.domain.FailEndState;
import io.seata.saga.statelang.domain.impl.FailEndStateImpl;
import io.seata.saga.statelang.parser.StateParser;

/**
 * Failed end state parser
 *
 * @author lorne.cl
 */
public class FailEndStateParser extends BaseStatePaser implements StateParser<FailEndState> {

    @Override
    public FailEndState parse(Object node) {

        FailEndStateImpl failEndState = new FailEndStateImpl();
        parseBaseAttributes(failEndState, node);

        Map<String, Object> nodeMap = (Map<String, Object>)node;
        failEndState.setErrorCode((String)nodeMap.get("ErrorCode"));
        failEndState.setMessage((String)nodeMap.get("Message"));

        return failEndState;
    }
}