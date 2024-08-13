/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.discovery.loadbalance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.config.ConfigurationFactory;

import static org.apache.seata.common.DefaultValues.VIRTUAL_NODES_DEFAULT;

/**
 * The type consistent hash load balance.
 */
@LoadLevel(name = LoadBalanceFactory.CONSISTENT_HASH_LOAD_BALANCE)
public class ConsistentHashLoadBalance implements LoadBalance {

    /**
     * The constant LOAD_BALANCE_CONSISTENT_HASH_VIRTUAL_NODES.
     */
    public static final String LOAD_BALANCE_CONSISTENT_HASH_VIRTUAL_NODES = LoadBalanceFactory.LOAD_BALANCE_PREFIX
        + "virtualNodes";
    /**
     * The constant VIRTUAL_NODES_NUM.
     */
    private static final int VIRTUAL_NODES_NUM = ConfigurationFactory.getInstance().getInt(
        LOAD_BALANCE_CONSISTENT_HASH_VIRTUAL_NODES, VIRTUAL_NODES_DEFAULT);

    @Override
    public <T> T select(List<T> invokers, String xid) {
        return new ConsistentHashSelector<>(invokers, VIRTUAL_NODES_NUM).select(xid);
    }

    private static final class ConsistentHashSelector<T> {

        private final SortedMap<Long, T> virtualInvokers = new TreeMap<>();
        private final HashFunction hashFunction = new SHA256Hash();

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

    private static class SHA256Hash implements HashFunction {
        MessageDigest instance;

        public SHA256Hash() {
            try {
                instance = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        @Override
        public long hash(String key) {
            instance.reset();
            instance.update(key.getBytes());
            byte[] digest = instance.digest(key.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            for (int i = 0; i < 8 && i < digest.length; i++) {
                hash <<= 8;
                hash |= digest[i] & 0xff;
            }
            return hash;
        }
    }

    /**
     * Hash String to long value
     */
    public interface HashFunction {
        long hash(String key);
    }
}
