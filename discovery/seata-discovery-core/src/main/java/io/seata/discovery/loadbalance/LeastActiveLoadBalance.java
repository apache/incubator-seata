package io.seata.discovery.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import io.seata.common.loader.LoadLevel;
import io.seata.common.rpc.RpcStatus;

/**
 * The type Random load balance.
 *
 * @author ph3636
 */
@LoadLevel(name = "LeastActiveLoadBalance", order = 4)
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    @Override
    protected <T> T doSelect(List<T> invokers) {
        int length = invokers.size();
        int leastActive = -1;
        int leastCount = 0;
        int[] leastIndexes = new int[length];
        for (int i = 0; i < length; i++) {
            int active = RpcStatus.getStatus(String.valueOf(invokers.get(i))).getActive();
            if (leastActive == -1 || active < leastActive) {
                leastActive = active;
                leastCount = 1;
                leastIndexes[0] = i;
            } else if (active == leastActive) {
                leastIndexes[leastCount++] = i;
            }
        }
        if (leastCount == 1) {
            return invokers.get(leastIndexes[0]);
        }
        return invokers.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }
}
