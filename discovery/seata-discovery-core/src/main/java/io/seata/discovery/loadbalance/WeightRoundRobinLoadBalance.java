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

import io.seata.common.loader.LoadLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round robin load balance with weight
 *
 * @author mawerss1@gmail.com
 * @Date 2019.08.12
 */
@LoadLevel(name = "WeightRoundRobinLoadBalance", order = 4)
public class WeightRoundRobinLoadBalance extends AbstractLoadBalance {

    AtomicInteger selectCount = new AtomicInteger();

    @Override
    protected <T extends ServerRegistration> T doSelect(List<T> invokers) {

        int gcd = invokers.get(0).getWeight();

        for (int i = 1; i < invokers.size(); i++) {
            int next = invokers.get(i).getWeight();
            gcd = findGcd(gcd, next);
        }
        ArrayList<T> pools = new ArrayList<>();


        if (gcd != 1) {
            for (ServerRegistration registration : invokers) {
                int j = registration.getWeight() / gcd;
                for (int i = 0; i < j; i++) {
                    pools.add((T)registration);
                }
            }
        }else{
            for (int i = 0; i < invokers.size(); i++) {
                for (int j = 0; j < invokers.get(i).getWeight(); j++) {
                    pools.add(invokers.get(i));
                }
            }
        }

        int result = getSequence() % pools.size();
        return pools.get(result);
    }


    public int getSequence() {
        for (; ; ) {
            int l = selectCount.get();
            int next = l >= Integer.MAX_VALUE ? 0 : l + 1;
            if (selectCount.compareAndSet(l, next)) {
                return l;
            }
        }
    }

    private int findGcd(int n, int m) {
        return (n == 0 || m == 0) ? n + m : findGcd(m, n % m);
    }

}
