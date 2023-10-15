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

import io.seata.common.ConfigurationKeys;
import io.seata.common.Constants;
import io.seata.common.XID;
import io.seata.config.ConfigValidator;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.MultiRegistryFactory;
import io.seata.discovery.registry.RegistryService;
import io.seata.server.auth.AbstractCheckAuthHandler;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.coordinator.DefaultCore;
import io.seata.server.session.GlobalSession;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Junduo Dong
 */
@Service
public class ConfigurationService {

    public boolean setRegistryConfiguration(String key, String value) {
        return false;
    }

    public boolean setConfigCenterConfiguration(String key, String value) {
        return false;
    }

    public boolean setRawConfiguration(String key, String value) {
        return false;
    }

    public Map<String, String> getConf(List<String> keys) {
        Configuration instance = ConfigurationFactory.getInstance();
        Configuration currentFileInstance = ConfigurationFactory.CURRENT_FILE_INSTANCE;
        Map<String, String> configurations = new HashMap<>();
        if (keys.size() == 0) {
            for (String confKey: ConfigValidator.availableConfiguration()) {
                configurations.put(confKey, instance.getConfig(confKey));
            }
            for (String confKey: ConfigValidator.availableRegistryConf()) {
                configurations.put(confKey, currentFileInstance.getConfig(confKey));
            }
            for (String confKey: ConfigValidator.availableConfigCenterConf()) {
                configurations.put(confKey, currentFileInstance.getConfig(confKey));
            }
        } else {
            for (String key: keys) {
                if (ConfigValidator.availableConfiguration().contains(key)) {
                    configurations.put(key, instance.getConfig(key));
                } else if (ConfigValidator.availableRegistryConf().contains(key) ||
                ConfigValidator.availableConfigCenterConf().contains(key)) {
                    currentFileInstance.getConfig(key);
                }
            }
        }
        return configurations;
    }

    /**
     * Reload configuration items used as static variables, which may be dynamically reconfigured
     */
    private synchronized void reloadConfigDynamically() {
        // Reload all configurations which can be dynamically configured
        DefaultCoordinator.reloadConfiguration();
        AbstractCheckAuthHandler.reloadConfiguration();
        GlobalSession.reloadConfiguration();
        DefaultCore.reloadConfiguration();
    }

    /**
     * Reload configuration instance
     */
    public synchronized void reloadConfigurationInstance() {
        ConfigurationFactory.reload();
        reloadConfigDynamically();
    }

    /**
     * Set config
     * @param conf new config
     * @return difference of old and new conf, type Map[key, [oldValue, newValue]]
     */
    public synchronized Map<String, List<String>> setConf(Map<String, String> conf) throws Exception {
        Map<String, String> oldConf = ConfigurationFactory.setConf(conf);
        reloadConfigDynamically();
        Map<String, List<String>> diff = new HashMap<>(oldConf.size());
        for (String key: oldConf.keySet()) {
            diff.put(key, new ArrayList<>(Arrays.asList(oldConf.get(key), conf.get(key))));
        }
        return diff;
    }

    /**
     * Set registry config
     * @param conf new config
     * @return difference of old and new conf, type Map[key, [oldValue, newValue]]
     */
    public synchronized Map<String, List<String>> setRegistryConf(Map<String, String> conf) throws Exception {
        List<RegistryService> oldServices = MultiRegistryFactory.getInstances();
        Map<String, String> oldConf = ConfigurationFactory.setRegistryConf(conf);
        Set<String> noNeedChangedTypes = getNoNeedChangedType(oldServices.stream()
                .map(RegistryService::getType).collect(Collectors.toSet()), oldConf, conf);
        InetSocketAddress address = new InetSocketAddress(XID.getIpAddress(), XID.getPort());

        List<RegistryService> unRegisteredServices = new ArrayList<>();
        List<RegistryService> newlyRegisteredServices = new ArrayList<>();

        try {
            // unregister affected registries
            for (RegistryService registryService : oldServices) {
                if (!noNeedChangedTypes.contains(registryService.getType())) {
                    registryService.unregister(address);
                    unRegisteredServices.add(registryService);
                }
            }
            MultiRegistryFactory.reloadRegistryServices();
            // register affected registries
            List<RegistryService> registryServices = MultiRegistryFactory.getInstances();
            for (RegistryService registryService : registryServices) {
                if (!noNeedChangedTypes.contains(registryService.getType())) {
                    registryService.register(address);
                    newlyRegisteredServices.add(registryService);
                }
            }
            Map<String, List<String>> diff = new HashMap<>(oldConf.size());
            for (String key: oldConf.keySet()) {
                diff.put(key, new ArrayList<>(Arrays.asList(oldConf.get(key), conf.get(key))));
            }
            return diff;
        } catch (Exception e) {
            // rollback
            ConfigurationFactory.setConf(oldConf);
            MultiRegistryFactory.reloadRegistryServices();
            for (RegistryService registryService : unRegisteredServices) {
                if (!noNeedChangedTypes.contains(registryService.getType())) {
                    registryService.register(address);
                }
            }
            for (RegistryService registryService : newlyRegisteredServices) {
                if (!noNeedChangedTypes.contains(registryService.getType())) {
                    registryService.unregister(address);
                }
            }
            throw e;
        }
    }

    private static Set<String> getNoNeedChangedType(Set<String> oldTypes, Map<String, String> changedOldConf,
                                                    Map<String, String> newConf) {
        Set<String> noNeedChangedTypes = oldTypes;
        String registryTypePrefix = ConfigurationKeys.FILE_ROOT_REGISTRY
                + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE;
        if (changedOldConf.containsKey(registryTypePrefix)) {
            Set<String> newTypes = Arrays.stream(newConf.get(registryTypePrefix)
                            .split(Constants.REGISTRY_TYPE_SPLIT_CHAR)).collect(Collectors.toSet());
            noNeedChangedTypes = oldTypes.stream().filter(newTypes::contains).collect(Collectors.toSet());
        }
        for (String key: changedOldConf.keySet()) {
            if (key.equals(registryTypePrefix)) continue;
            String type = key.split("\\.")[1];
            noNeedChangedTypes.remove(type);
        }
        return noNeedChangedTypes;
    }

    /**
     * Set config center conf
     * @param conf new config
     * @return difference of old and new conf, type Map[key, [oldValue, newValue]]
     */
    public synchronized Map<String, List<String>> setConfCenterConf(Map<String, String> conf) throws Exception {
        Map<String, String> oldConf = ConfigurationFactory.setConfCenterConf(conf);
        Map<String, List<String>> diff = new HashMap<>(oldConf.size());
        for (String key: oldConf.keySet()) {
            diff.put(key, new ArrayList<>(Arrays.asList(oldConf.get(key), conf.get(key))));
        }
        return diff;
    }
}
