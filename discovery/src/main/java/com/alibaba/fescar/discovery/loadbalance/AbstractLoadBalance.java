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
package com.alibaba.fescar.discovery.loadbalance;

import java.util.List;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;

/**
 * The type Abstract load balance.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /02/12
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    public <T> T select(List<T> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        return doSelect(invokers);
    }

    /**
     * Do select t.
     *
     * @param <T>      the type parameter
     * @param invokers the invokers
     * @return the t
     */
    protected abstract <T> T doSelect(List<T> invokers);
}
