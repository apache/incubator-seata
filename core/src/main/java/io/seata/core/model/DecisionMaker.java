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
package io.seata.core.model;

import java.util.HashMap;
import java.util.Map;

import io.seata.common.exception.ShouldNeverHappenException;

public enum DecisionMaker {
    TC(0),
    TM(1);

    private int code;

    DecisionMaker(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    private static final Map<Integer, DecisionMaker> MAP = new HashMap<>(values().length);

    static {
        for (DecisionMaker decisionMaker : values()) {
            MAP.put(decisionMaker.getCode(), decisionMaker);
        }
    }

    public static DecisionMaker get(int code) {
        DecisionMaker status = MAP.get(code);
        if (null == status) {
            throw new ShouldNeverHappenException("Unknown DecisionMaker[" + code + "]");
        }

        return status;
    }
}
