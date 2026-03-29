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
package org.apache.seata.core.rpc.netty.loadbalance;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.rpc.RpcContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Consistent hash load balance for server side.
 */
@LoadLevel(name = "ConsistentHashLoadBalance")
public class ServerConsistentHashLoadBalance implements ServerLoadBalance {

    public static final String LOAD_BALANCE_CONSISTENT_HASH_VIRTUAL_NODES = "server.loadBalance.virtualNodes";

    private static final int VIRTUAL_NODES_NUM = ConfigurationFactory.getInstance()
            .getInt(LOAD_BALANCE_CONSISTENT_HASH_VIRTUAL_NODES, DefaultValues.VIRTUAL_NODES_DEFAULT);

    private volatile ConsistentHashSelectorWrapper selectorWrapper;

    @Override
    public RpcContext select(List<RpcContext> candidates, String xid) {
        if (selectorWrapper == null) {
            synchronized (this) {
                if (selectorWrapper == null) {
                    selectorWrapper =
                            new ConsistentHashSelectorWrapper(new ConsistentHashSelector(candidates, VIRTUAL_NODES_NUM), candidates);
                }
            }
        }
        String objectKey = StringUtils.isNotBlank(xid) ? xid : buildStableFallbackKey(candidates);
        return selectorWrapper.getSelector(candidates).select(objectKey);
    }

    private String buildStableFallbackKey(List<RpcContext> candidates) {
        return candidates.stream()
                .map(RpcContext::getClientId)
                .filter(StringUtils::isNotBlank)
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private static final class ConsistentHashSelectorWrapper {

        private volatile ConsistentHashSelector selector;
        private volatile Set<RpcContext> candidates;

        private ConsistentHashSelectorWrapper(ConsistentHashSelector selector, List<RpcContext> candidates) {
            this.selector = selector;
            this.candidates = new HashSet<>(candidates);
        }

        private ConsistentHashSelector getSelector(List<RpcContext> newCandidates) {
            if (!equals(newCandidates)) {
                synchronized (this) {
                    if (!equals(newCandidates)) {
                        selector = new ConsistentHashSelector(newCandidates, VIRTUAL_NODES_NUM);
                        this.candidates = new HashSet<>(newCandidates);
                    }
                }
            }
            return selector;
        }

        private boolean equals(List<RpcContext> newCandidates) {
            if (newCandidates.size() != this.candidates.size()) {
                return false;
            }
            for (RpcContext candidate : newCandidates) {
                if (!this.candidates.contains(candidate)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class ConsistentHashSelector {

        private final SortedMap<Long, RpcContext> virtualInvokers = new TreeMap<>();
        private final HashFunction hashFunction = new SHA256Hash();

        private ConsistentHashSelector(List<RpcContext> candidates, int virtualNodes) {
            for (RpcContext candidate : candidates) {
                for (int i = 0; i < virtualNodes; i++) {
                    virtualInvokers.put(hashFunction.hash(candidate.toString() + i), candidate);
                }
            }
        }

        private RpcContext select(String objectKey) {
            SortedMap<Long, RpcContext> tailMap = virtualInvokers.tailMap(hashFunction.hash(objectKey));
            Long nodeHashVal = tailMap.isEmpty() ? virtualInvokers.firstKey() : tailMap.firstKey();
            return virtualInvokers.get(nodeHashVal);
        }
    }

    private static class SHA256Hash implements HashFunction {

        private static final ThreadLocal<MessageDigest> INSTANCE =
                ThreadLocal.withInitial(() -> {
                    try {
                        return MessageDigest.getInstance("SHA-256");
                    } catch (NoSuchAlgorithmException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                });

        @Override
        public long hash(String key) {
            MessageDigest instance = INSTANCE.get();
            instance.reset();
            instance.update(key.getBytes(StandardCharsets.UTF_8));
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
     * Hash string to long value.
     */
    public interface HashFunction {
        long hash(String key);
    }
}
