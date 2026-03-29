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
package org.apache.seata.core.rpc.netty.loadbalance;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.rpc.RpcContext;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Least active load balance for server side.
 */
@LoadLevel(name = "LeastActiveLoadBalance")
public class ServerLeastActiveLoadBalance implements ServerLoadBalance {

    @Override
    public RpcContext select(List<RpcContext> candidates, String xid) {
        long leastActive = -1;
        int leastCount = 0;
        int[] leastIndexes = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            long active = candidates.get(i).getActiveCount();
            if (leastActive == -1 || active < leastActive) {
                leastActive = active;
                leastCount = 1;
                leastIndexes[0] = i;
            } else if (active == leastActive) {
                leastIndexes[leastCount++] = i;
            }
        }
        if (leastCount == 1) {
            return candidates.get(leastIndexes[0]);
        }
        return candidates.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }
}
