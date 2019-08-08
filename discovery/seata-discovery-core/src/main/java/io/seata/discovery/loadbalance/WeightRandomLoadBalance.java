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
