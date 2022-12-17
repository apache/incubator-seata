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

import io.seata.common.executor.Callback;
import io.seata.commonapi.util.ProxyUtil;
import io.seata.rm.RMClient;
import io.seata.tm.TMClient;

import java.lang.reflect.InvocationTargetException;

public class SeataClient {

    public static void init(String applicationId, String txServiceGroup) {
        TMClient.init(applicationId, txServiceGroup);
        RMClient.init(applicationId, txServiceGroup);
    }

    /**
     * @param target
     * @param <T>
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static <T> T createProxy(T target) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        return ProxyUtil.createProxy(target);
    }

    public static void execute(Callback<Object> targetCallback){



    }

    /**
     * register a branch source
     */
    public static <T> T registerBranchSource(T target) {
        //1、注册分支服务
        //2、创建增强代理
        return ProxyUtil.createProxy(target);
    }
}
