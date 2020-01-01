package io.seata.discovery.loadbalance;

import io.seata.common.loader.LoadLevel;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The type Random load balance.
 *
 * @author ph3636
 */
@LoadLevel(name = "LeastActiveLoadBalance", order = 4)
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    private static final ConcurrentMap<String, AtomicInteger> ACTIVE_MAP = new ConcurrentHashMap<>();

    @Override
    protected <T> T doSelect(List<T> invokers) {
        int leastActive = -1;
        int leastIndex = 0;
        for (int i = 0; i < invokers.size(); i++) {
            int active = ACTIVE_MAP.computeIfAbsent(String.valueOf(invokers.get(i)),
                    e -> new AtomicInteger(0)).get();
            if (leastActive == -1 || active < leastActive) {
                leastActive = active;
                leastIndex = i;
            }
        }
        addActive(ACTIVE_MAP.get(String.valueOf(invokers.get(leastIndex))));
        return invokers.get(leastIndex);
    }

    private void addActive(AtomicInteger active) {
        for (; ; ) {
            int current = active.get();
            int next = current >= Integer.MAX_VALUE ? 0 : current + 1;
            if (active.compareAndSet(current, next)) {
                return;
            }
        }
    }
}
