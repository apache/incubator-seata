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

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class WeightRandomLoadBalanceTest extends LoadBalanceTest{

    @Test
    public void testWeightLoadBalance(){
        int runNum = 1000;
        ArrayList list = new ArrayList<ServerRegistration>();
        ServerRegistration serverRegistration = new ServerRegistration(new InetSocketAddress("127.0.0.1",8081), 20);
        ServerRegistration serverRegistration1 = new ServerRegistration(new InetSocketAddress("127.0.0.1",8082), 30);
        ServerRegistration serverRegistration2 = new ServerRegistration(new InetSocketAddress("127.0.0.1",8083), 50);
        list.add(serverRegistration);
        list.add(serverRegistration1);
        list.add(serverRegistration2);

        WeightRandomLoadBalance weightRandomLoadBalance = new WeightRandomLoadBalance();
        Map<ServerRegistration, AtomicLong> selectedCounter = getSelectedCounter(runNum, list, weightRandomLoadBalance);

        Iterator<Map.Entry<ServerRegistration, AtomicLong>> iterator = selectedCounter.entrySet().iterator();
        int totalRunNum = 0;
        while (iterator.hasNext()) {
            Map.Entry<ServerRegistration, AtomicLong> en = iterator.next();
            ServerRegistration key = en.getKey();
            Long count = selectedCounter.get(key).get();
            totalRunNum += count;
        }
        Assertions.assertEquals(totalRunNum,runNum);
    }

}
