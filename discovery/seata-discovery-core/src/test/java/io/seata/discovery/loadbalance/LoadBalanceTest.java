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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Created by guoyao on 2019/2/14.
 */
public class LoadBalanceTest {

    /**
     * Test random load balance select.
     *
     * @param serverRegistrations the serverRegistrations
     */
    @ParameterizedTest
    @MethodSource("addressProvider")
    public void testRandomLoadBalance_select(List<ServerRegistration> serverRegistrations) {
        int runs = 10000;
        Map<ServerRegistration, AtomicLong> counter = getSelectedCounter(runs, serverRegistrations, new RandomLoadBalance());
        for (ServerRegistration serverRegistration : counter.keySet()) {
            Long count = counter.get(serverRegistration).get();
            Assertions.assertTrue(count > 0, "selecte one time at last");
        }
    }

    /**
     * Test round robin load balance select.
     *
     * @param serverRegistrations the serverRegistrations
     */
    @ParameterizedTest
    @MethodSource("addressProvider")
    public void testRoundRobinLoadBalance_select(List<ServerRegistration> serverRegistrations) {
        int runs = 10000;
        Map<ServerRegistration, AtomicLong> counter = getSelectedCounter(runs, serverRegistrations, new RoundRobinLoadBalance());
        for (ServerRegistration serverRegistration : counter.keySet()) {
            Long count = counter.get(serverRegistration).get();
            Assertions.assertTrue(Math.abs(count - runs / (0f + serverRegistrations.size())) < 1f, "abs diff shoud < 1");
        }
    }

    /**
     * Gets selected counter.
     *
     * @param runs        the runs
     * @param serverRegistrations   the serverRegistrations
     * @param loadBalance the load balance
     * @return the selected counter
     */
    public Map<ServerRegistration, AtomicLong> getSelectedCounter(int runs, List<ServerRegistration> serverRegistrations,
                                                                  LoadBalance loadBalance) {
        Assertions.assertNotNull(loadBalance);
        Map<ServerRegistration, AtomicLong> counter = new ConcurrentHashMap<>();
        for (ServerRegistration serverRegistration : serverRegistrations) {
            counter.put(serverRegistration, new AtomicLong(0));
        }
        try {
            for (int i = 0; i < runs; i++) {
                ServerRegistration selectServerRegistration = loadBalance.select(serverRegistrations);
                counter.get(selectServerRegistration).incrementAndGet();
            }
        } catch (Exception e) {
            //do nothing
        }
        return counter;
    }

    /**
     * ServerRegistration provider object [ ] [ ].
     *
     * @return Stream<List < InetSocketAddress>>
     */
    static Stream<List<ServerRegistration>> addressProvider() {
        return Stream.of(
                Arrays.asList(new ServerRegistration(new InetSocketAddress("127.0.0.1", 8091),0),
                        new ServerRegistration(new InetSocketAddress("127.0.0.1", 8092),0),
                        new ServerRegistration(new InetSocketAddress("127.0.0.1", 8093),0),
                        new ServerRegistration(new InetSocketAddress("127.0.0.1", 8094),0),
                        new ServerRegistration(new InetSocketAddress("127.0.0.1", 8095),0)
        ));
    }

    /**
     *
     * @return Map<ServerRegistration, Integer>
     */
    static Map<ServerRegistration, Integer> registrationProvider() {
        return new HashMap<ServerRegistration,Integer>(){
            {
                put(new ServerRegistration(new InetSocketAddress("127.0.0.1", 8091), 10), 10);
                put(new ServerRegistration(new InetSocketAddress("127.0.0.1", 8092), 20), 20);
                put(new ServerRegistration(new InetSocketAddress("127.0.0.1", 8093), 30), 30);
                put(new ServerRegistration(new InetSocketAddress("127.0.0.1", 8094), 40), 40);
            }
        };
    }

}
