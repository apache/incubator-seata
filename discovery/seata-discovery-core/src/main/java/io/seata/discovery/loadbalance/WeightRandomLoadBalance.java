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

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeightRandomLoadBalance extends AbstractLoadBalance{

    @Override
    protected <T extends ServerRegistration> T doSelect(List<T> invokers) {

        boolean sameWeight = true;
        int totalWeight = 0;
        List<T> s = invokers;

        s.sort(new Comparator<ServerRegistration>() {
            @Override
            public int compare(ServerRegistration o1, ServerRegistration o2) {
                return o2.getWeight() - o1.getWeight();
            }
        });

        int firstWeight = 0;
        for (int i = 0; i < s.size(); i++) {
            ServerRegistration serverRegistration = s.get(i);
            int curWeight = serverRegistration.getWeight();
            if (i == 0) {
                firstWeight = curWeight;
            }
            if (sameWeight && curWeight != firstWeight) {
                sameWeight = false;
            }

            totalWeight += curWeight;
        }

        if (totalWeight > 0 && !sameWeight) {
            int random = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < s.size(); i++) {
                if (random >= s.get(i).getWeight()) {
                    return s.get(i);
                }
            }
        }

        return invokers.get(ThreadLocalRandom.current().nextInt(s.size()));
    }
}
