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

package io.seata.server.console.service;

import io.seata.common.XID;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.MultiRegistryFactory;
import io.seata.discovery.registry.RegistryService;
import io.seata.server.auth.AbstractCheckAuthHandler;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.coordinator.DefaultCore;
import io.seata.server.session.GlobalSession;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author Junduo Dong
 */
@Service
public class ConfigurationService {

    public synchronized void reloadConfiguration() {
        ConfigurationFactory.reload();

        // Reload all config which can be dynamically configured
        DefaultCoordinator.reloadConfiguration();
        AbstractCheckAuthHandler.reloadConfiguration();
        GlobalSession.reloadConfiguration();
        DefaultCore.reloadConfiguration();
    }

    public boolean reloadRegistryConfiguration() {
        reloadConfiguration();
        MultiRegistryFactory.reloadRegistryServices();

        try {
            List<RegistryService> registryServices = MultiRegistryFactory.getInstances();
            InetSocketAddress address = new InetSocketAddress(XID.getIpAddress(), XID.getPort());
            for (RegistryService registryService : registryServices) {
                registryService.register(address);
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }



}
