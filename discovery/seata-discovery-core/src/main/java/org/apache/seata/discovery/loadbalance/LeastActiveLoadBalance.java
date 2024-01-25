/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.discovery.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.rpc.RpcStatus;

import static org.apache.seata.discovery.loadbalance.LoadBalanceFactory.LEAST_ACTIVE_LOAD_BALANCE;

/**
 * The type Least Active load balance.
 *
 */
@LoadLevel(name = LEAST_ACTIVE_LOAD_BALANCE)
public class LeastActiveLoadBalance implements LoadBalance {

    @Override
    public <T> T select(List<T> invokers, String xid) {
        int length = invokers.size();
        long leastActive = -1;
        int leastCount = 0;
        int[] leastIndexes = new int[length];
        for (int i = 0; i < length; i++) {
            long active = RpcStatus.getStatus(invokers.get(i).toString()).getActive();
            if (leastActive == -1 || active < leastActive) {
                leastActive = active;
                leastCount = 1;
                leastIndexes[0] = i;
            } else if (active == leastActive) {
                leastIndexes[leastCount++] = i;
            }
        }
        if (leastCount == 1) {
            return invokers.get(leastIndexes[0]);
        }
        return invokers.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }
}
