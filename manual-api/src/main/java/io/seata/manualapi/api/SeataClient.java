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
package io.seata.manualapi.api;

import io.seata.commonapi.interceptor.parser.DefaultResourceRegisterParser;
import io.seata.commonapi.util.ProxyUtil;
import io.seata.rm.RMClient;
import io.seata.tm.TMClient;

public class SeataClient {

    public static void init(String applicationId, String txServiceGroup) {
        TMClient.init(applicationId, txServiceGroup);
        RMClient.init(applicationId, txServiceGroup);
    }

    /**
     * @param target
     * @param <T>
     * @return
     */
    public static <T> T createProxy(T target) {
        return ProxyUtil.createProxy(target);
    }

    /**
     * register a branch source
     */
    public static void registerBranchSource(Object target) {
        DefaultResourceRegisterParser.get().registerResource(target);
    }
}
