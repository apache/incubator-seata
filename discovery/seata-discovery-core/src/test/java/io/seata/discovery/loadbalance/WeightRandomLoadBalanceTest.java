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
        ServerRegistration serverRegistration = new ServerRegistration(new InetSocketAddress("",2), 20);
        ServerRegistration serverRegistration1 = new ServerRegistration(new InetSocketAddress("",2), 30);
        ServerRegistration serverRegistration2 = new ServerRegistration(new InetSocketAddress("",2), 50);
        list.add(serverRegistration);
        list.add(serverRegistration1);
        list.add(serverRegistration2);

        WeightRandomLoadBalance weightRandomLoadBalance = new WeightRandomLoadBalance();
        Map<ServerRegistration, AtomicLong> selectedCounter = getSelectedCounter(runNum, list, weightRandomLoadBalance);
        for (int i = 0; i < 100; i++) {
            ServerRegistration s = weightRandomLoadBalance.doSelect(list);
            AtomicLong count = selectedCounter.get(s);
            count.incrementAndGet();
        }

        Iterator<Map.Entry<ServerRegistration, AtomicLong>> iterator = selectedCounter.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ServerRegistration, AtomicLong> en = iterator.next();
            ServerRegistration key = en.getKey();
            Long count = selectedCounter.get(key).get();
            Integer weight = key.getWeight();

            // 允许误差在10%以内
            float percert = (float)count / (float)runNum;
            float weightPer = (float)weight / 100f;
            Assertions.assertTrue(percert > (weightPer - 0.2 )&& percert < (weightPer + 0.2));
        }
    }

}
