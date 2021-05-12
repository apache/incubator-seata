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
package io.seata.discovery.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import io.seata.common.loader.LoadLevel;
import io.seata.common.rpc.RpcStatus;

import static io.seata.discovery.loadbalance.LoadBalanceFactory.LEAST_ACTIVE_LOAD_BALANCE;

/**
 * The type Least Active load balance.
 *
 * @author ph3636
 */
@LoadLevel(name = LEAST_ACTIVE_LOAD_BALANCE)
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    @Override
    protected <T> T doSelect(List<T> invokers, String xid) {
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
