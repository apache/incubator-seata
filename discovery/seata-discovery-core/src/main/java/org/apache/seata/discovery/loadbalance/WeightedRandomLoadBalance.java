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

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.metadata.ServiceInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The type Weighted random load balance(based on metadata).
 *
 * Weight rules:
 * - Only supports non-negative weights (>= 0)
 * - Weight can be configured in metadata with key "weight" as Number or String
 * - Default weight is 1 if not specified
 */
@LoadLevel(name = LoadBalanceFactory.WEIGHTED_RANDOM_LOAD_BALANCE)
public class WeightedRandomLoadBalance implements LoadBalance {

    private static final LoadBalance RANDOM_LOAD_BALANCE =
            EnhancedServiceLoader.load(LoadBalance.class, LoadBalanceFactory.RANDOM_LOAD_BALANCE);

    private static final String WEIGHT_KEY = "weight";
    private static final int DEFAULT_WEIGHT = 1;

    @Override
    public <T> T select(List<T> invokers, String xid) throws Exception {
        // Check if all instances have no weight, if so downgrade to random load balancing
        if (!hasAnyWeight(invokers)) {
            return RANDOM_LOAD_BALANCE.select(invokers, xid);
        }

        // Calculate total weight
        int totalWeight = calculateTotalWeight(invokers);

        // If total weight is 0, downgrade to random load balancing
        if (totalWeight <= 0) {
            return RANDOM_LOAD_BALANCE.select(invokers, xid);
        }

        // Generate random numbers
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);

        // Select instances based on weights
        int currentWeight = 0;
        for (T invoker : invokers) {
            if (invoker instanceof ServiceInstance) {
                ServiceInstance instance = (ServiceInstance) invoker;
                int weight = getWeight(instance);
                currentWeight += weight;
                if (randomWeight < currentWeight) {
                    return invoker;
                }
            }
        }

        return invokers.get(0);
    }

    /**
     * Check if any instances contain valid weight information (weight > 0)
     * @param invokers Instance List
     * @return Returns true if any instance contains valid weight information
     */
    private <T> boolean hasAnyWeight(List<T> invokers) {
        for (T invoker : invokers) {
            if (invoker instanceof ServiceInstance) {
                ServiceInstance instance = (ServiceInstance) invoker;
                if (instance.getMetadata() != null && instance.getMetadata().containsKey(WEIGHT_KEY)) {
                    int weight = getWeight(instance);
                    if (weight > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Calculate the total weight of all instances
     * @param invokers Instance List
     * @return Total weight
     */
    private <T> int calculateTotalWeight(List<T> invokers) {
        int totalWeight = 0;
        for (T invoker : invokers) {
            if (invoker instanceof ServiceInstance) {
                ServiceInstance instance = (ServiceInstance) invoker;
                totalWeight += getWeight(instance);
            }
        }
        return totalWeight;
    }

    /**
     * Get the weight of the instance
     * @param instance Instance
     * @return Weight value, if not set returns the default weight of 1, negative weights are treated as 0
     */
    private int getWeight(ServiceInstance instance) {
        if (instance.getMetadata() != null) {
            Object weightObj = instance.getMetadata().get(WEIGHT_KEY);
            if (weightObj != null) {
                int weight = 0;
                if (weightObj instanceof Number) {
                    weight = ((Number) weightObj).intValue();
                } else if (weightObj instanceof String) {
                    try {
                        weight = Integer.parseInt((String) weightObj);
                    } catch (NumberFormatException e) {
                        return DEFAULT_WEIGHT;
                    }
                }

                // Only return non-negative weights
                return Math.max(0, weight);
            }
        }
        return DEFAULT_WEIGHT;
    }
}
