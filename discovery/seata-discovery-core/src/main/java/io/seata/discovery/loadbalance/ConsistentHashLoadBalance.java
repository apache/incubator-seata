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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import io.seata.common.loader.LoadLevel;
import io.seata.config.ConfigurationFactory;

import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.config.ConfigurationKeys.FILE_ROOT_REGISTRY;
import static io.seata.common.DefaultValues.VIRTUAL_NODES_DEFAULT;

/**
 * The type consistent hash load balance.
 *
 * @author ph3636
 */
@LoadLevel(name = "ConsistentHashLoadBalance")
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    private static final String VIRTUAL_NODES = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + "loadBalanceVirtualNodes";
    private static final int VIRTUAL_NODES_NUM = ConfigurationFactory.CURRENT_FILE_INSTANCE.getInt(VIRTUAL_NODES, VIRTUAL_NODES_DEFAULT);

    @Override
    protected <T> T doSelect(List<T> invokers, String xid) {
        return new ConsistentHashSelector<>(invokers, VIRTUAL_NODES_NUM).select(xid);
    }

    private static final class ConsistentHashSelector<T> {

        private final SortedMap<Long, T> virtualInvokers = new TreeMap<>();
        private final HashFunction hashFunction = new MD5Hash();

        ConsistentHashSelector(List<T> invokers, int virtualNodes) {
            for (T invoker : invokers) {
                for (int i = 0; i < virtualNodes; i++) {
                    virtualInvokers.put(hashFunction.hash(invoker.toString() + i), invoker);
                }
            }
        }

        public T select(String objectKey) {
            SortedMap<Long, T> tailMap = virtualInvokers.tailMap(hashFunction.hash(objectKey));
            Long nodeHashVal = tailMap.isEmpty() ? virtualInvokers.firstKey() : tailMap.firstKey();
            return virtualInvokers.get(nodeHashVal);
        }
    }

    private static class MD5Hash implements HashFunction {
        MessageDigest instance;
        public MD5Hash() {
            try {
                instance = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        @Override
        public long hash(String key) {
            instance.reset();
            instance.update(key.getBytes());
            byte[] digest = instance.digest();
            long h = 0;
            for (int i = 0; i < 4; i++) {
                h <<= 8;
                h |= ((int) digest[i]) & 0xFF;
            }
            return h;
        }
    }

    /**
     * Hash String to long value
     */
    public interface HashFunction {
        long hash(String key);
    }
}
