/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package io.seata.core.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import io.seata.common.exception.ShouldNeverHappenException;
import org.apache.commons.lang.StringUtils;

/**
 * State Graph
 * <p>allow list and forbidden list
 *
 * @author leizhiyuan
 */
public class StateGraph implements Serializable {

    private static final long serialVersionUID = -6419979524773660266L;
    /**
     * allow list
     */
    private Set<String> allowStates = new HashSet<String>();

    /**
     * forbidden list
     */
    private Set<String> forbidStates = new HashSet<String>();


    /**
     * add allow status
     *
     * @param state status
     */
    public void addAllowState(String state) {
        allowStates.add(state);
    }

    /**
     * add forbidden status
     *
     * @param state status
     */
    public void addForbidState(String state) {
        forbidStates.add(state);
    }

    /**
     *
     * forbidden > allow
     *
     * @param state target status
     * @return islegal
     */
    public boolean analyse(String state) {

        assertNotBlank(state, "the state to check is empty!");

        if (state.startsWith(StateGraphHelper.FORBIDDEN_STATUS)) {
            return analyseBlackStatus(state);
        }
        return analyseAllowStatus(state);

    }

    private void assertNotBlank(String state, String message) {
        if (StringUtils.isEmpty(state)) {
            throw new ShouldNeverHappenException(message);
        }
    }

    /**
     * judge allow status
     *
     * @param state target status
     * @return islegal
     */
    private boolean analyseAllowStatus(String state) {
        return allowStates.contains(state);

    }

    /**
     * judge forbidden status
     *
     * @param state target status
     * @return islegal
     */
    private boolean analyseBlackStatus(String state) {

        return !forbidStates.contains(state);

    }
}
