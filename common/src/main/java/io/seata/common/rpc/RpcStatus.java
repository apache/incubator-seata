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
package io.seata.common.rpc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * The state statistics.
 *
 * @author ph3636
 */
public class RpcStatus {

    private static final ConcurrentMap<String, RpcStatus> SERVICE_STATUS_MAP = new ConcurrentHashMap<>();
    private final AtomicLong active = new AtomicLong();
    private final LongAdder total = new LongAdder();

    private RpcStatus() {
    }

    /**
     * get the RpcStatus of this service
     *
     * @param service the service
     * @return RpcStatus
     */
    public static RpcStatus getStatus(String service) {
        return SERVICE_STATUS_MAP.computeIfAbsent(service, key -> new RpcStatus());
    }

    /**
     * remove the RpcStatus of this service
     *
     * @param service the service
     */
    public static void removeStatus(String service) {
        SERVICE_STATUS_MAP.remove(service);
    }

    /**
     * begin count
     *
     * @param service the service
     */
    public static void beginCount(String service) {
        getStatus(service).active.incrementAndGet();
    }

    /**
     * end count
     *
     * @param service the service
     */
    public static void endCount(String service) {
        RpcStatus rpcStatus = getStatus(service);
        rpcStatus.active.decrementAndGet();
        rpcStatus.total.increment();
    }

    /**
     * get active.
     *
     * @return active
     */
    public long getActive() {
        return active.get();
    }

    /**
     * get total.
     *
     * @return total
     */
    public long getTotal() {
        return total.longValue();
    }
}
