package com.alibaba.fescar.core.service;

import java.util.List;

/**
 * Service Registry and Discovery
 */
public interface ServiceManager {

    /**
     * Register address to txServiceGroup server list.
     * @param txServiceGroup transaction service group (service name)
     * @param address server node address
     */
    void register(String txServiceGroup, String address);

    /**
     * Unregister address to txServiceGroup server list.
     * @param txServiceGroup transaction service group (service name)
     * @param address server node address
     */
    void unregister(String txServiceGroup, String address);

    /**
     * Watch service addresses for the give txServiceGroup
     * @param txServiceGroup transaction service group (service name)
     * @param watcher service address watcher
     */
    void watch(String txServiceGroup, AddressWatcher watcher);

    /**
     * Lookup server addresses for the given txServiceGroup
     * @param txServiceGroup transaction service group (service name)
     * @return available addresses
     */
    String[] lookup(String txServiceGroup);
}
