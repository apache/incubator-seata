/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.service;

/**
 * Service Registry and Discovery
 */
public interface ServiceManager {

    /**
     * Register address to txServiceGroup server list.
     *
     * @param txServiceGroup transaction service group (service name)
     * @param address        server node address
     */
    void register(String txServiceGroup, String address);

    /**
     * Unregister address to txServiceGroup server list.
     *
     * @param txServiceGroup transaction service group (service name)
     * @param address        server node address
     */
    void unregister(String txServiceGroup, String address);

    /**
     * Watch service addresses for the give txServiceGroup
     *
     * @param txServiceGroup transaction service group (service name)
     * @param watcher        service address watcher
     */
    void watch(String txServiceGroup, AddressWatcher watcher);

    /**
     * Lookup server addresses for the given txServiceGroup
     *
     * @param txServiceGroup transaction service group (service name)
     * @return available addresses
     */
    String[] lookup(String txServiceGroup);
}
