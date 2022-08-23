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
package io.seata.discovery.registry.polaris;

import io.seata.discovery.registry.RegistryService;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * {@link PolarisRegistryServiceImpl} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-23
 */
public class PolarisRegistryServiceImpl implements RegistryService<PolarisListener> {

    public static RegistryService getInstance() {
        return new PolarisRegistryServiceImpl();
    }

    /**
     * Register.
     *
     * @param address the address
     * @throws Exception the exception
     */
    @Override public void register(InetSocketAddress address) throws Exception {

    }

    /**
     * Unregister.
     *
     * @param address the address
     * @throws Exception the exception
     */
    @Override public void unregister(InetSocketAddress address) throws Exception {

    }

    /**
     * Subscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    @Override public void subscribe(String cluster, PolarisListener listener) throws Exception {

    }

    /**
     * Unsubscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    @Override public void unsubscribe(String cluster, PolarisListener listener) throws Exception {

    }

    /**
     * Lookup list.
     *
     * @param key the key
     * @return the list
     * @throws Exception the exception
     */
    @Override public List<InetSocketAddress> lookup(String key) throws Exception {
        return null;
    }

    /**
     * Close.
     *
     * @throws Exception
     */
    @Override public void close() throws Exception {

    }
}
