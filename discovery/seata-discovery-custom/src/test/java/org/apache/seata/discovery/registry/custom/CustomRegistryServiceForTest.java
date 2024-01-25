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
package org.apache.seata.discovery.registry.custom;

import org.apache.seata.config.ConfigChangeListener;
import org.apache.seata.discovery.registry.RegistryService;

import java.net.InetSocketAddress;
import java.util.List;

public class CustomRegistryServiceForTest implements RegistryService<ConfigChangeListener> {
    @Override
    public void register(InetSocketAddress address) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void subscribe(String cluster, ConfigChangeListener listener) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unsubscribe(String cluster, ConfigChangeListener listener) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws Exception {
        throw new UnsupportedOperationException();
    }

}
