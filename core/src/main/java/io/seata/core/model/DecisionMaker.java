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

import io.seata.common.util.StringUtils;

/**
 * The enum decision maker
 * @author zjinlei
 */
public enum DecisionMaker {

    /**
     * The TC
     */
    TC,
    /**
     * The TM
     */
    TM;

    public static DecisionMaker get(String name) {
        if (StringUtils.isNullOrEmpty(name)) {
            return null;
        }
        for (DecisionMaker decisionMaker : DecisionMaker.values()) {
            if (decisionMaker.name().equals(name)) {
                return decisionMaker;
            }
        }
        throw new IllegalArgumentException("Unknown DecisionMaker[" + name + "]");
    }
}
