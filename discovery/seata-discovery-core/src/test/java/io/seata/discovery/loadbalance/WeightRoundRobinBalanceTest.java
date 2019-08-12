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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

class WeightRoundRobinBalanceTest extends LoadBalanceTest{

    @Test
    public void testWeightRoundRobinBalance() {
        ArrayList<ServerRegistration> list = new ArrayList<>(registrationProvider().keySet());
        int totalWeight = 0;
        for (ServerRegistration registration : list) {
            totalWeight += registration.getWeight();
        }
        int runs = 1000;
        Map<ServerRegistration, AtomicLong> selectedCounter = getSelectedCounter(runs, list, new WeightRoundRobinLoadBalance());
        for (Map.Entry<ServerRegistration,Integer> entry : registrationProvider().entrySet()){
            AtomicLong atomicLong = selectedCounter.get(entry.getKey());
            Assertions.assertEquals(runs * entry.getValue() / totalWeight,atomicLong.get());
        }
    }
}