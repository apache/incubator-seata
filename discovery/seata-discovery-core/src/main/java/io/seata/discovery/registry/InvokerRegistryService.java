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
package io.seata.discovery.registry;

import io.seata.discovery.loadbalance.ServerRegistration;

import java.util.List;

public interface InvokerRegistryService extends RegistryService{

    /**
     * register the serverRegistration
     *
     * @param serverRegistration
     * @throws Exception
     */
    void registerInvoker(ServerRegistration serverRegistration) throws Exception;

    /**
     * unregister the serverRegistration
     *
     * @param serverRegistration
     * @throws Exception
     */
    void unRegisterInvoker(ServerRegistration serverRegistration) throws Exception;
    /**
     * Lookup list.
     *
     * @param key the key
     * @return the list
     * @throws Exception the exception
     */
    List<ServerRegistration> lookup(String key) throws Exception;

    /**
     * Close.
     * @throws Exception
     */
    void close() throws Exception;
}
