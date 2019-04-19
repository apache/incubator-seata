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

import org.apache.commons.lang.StringUtils;

/**
 * State Graph Helper
 *
 * @author leizhiyuan
 */
public class StateGraphHelper {

    public final static String ACCEPT_STATUS = "APT-";

    public final static String FORBIDDEN_STATUS = "BL-";

    private final static String SEP = "-";

    /**
     * struct accpet state
     *
     * @param keys key list
     * @return state
     */
    public static String toAcceptState(String... keys) {

        return ACCEPT_STATUS + StringUtils.join(keys, SEP);

    }

    /**
     * struct forbidden state
     *
     * @param keys key list
     * @return state
     */
    public static String toBlackState(String... keys) {
        return FORBIDDEN_STATUS + StringUtils.join(keys, SEP);
    }

}
