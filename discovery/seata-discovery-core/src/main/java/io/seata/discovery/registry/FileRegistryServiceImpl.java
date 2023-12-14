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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import io.seata.common.util.StringUtils;
import io.seata.config.ConfigChangeListener;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.exception.ConfigNotFoundException;

/**
 * The type File registry service.
 *
 * @author slievrly
 */
public class FileRegistryServiceImpl implements RegistryService<ConfigChangeListener> {

    private static volatile FileRegistryServiceImpl instance;
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();
    private static final String POSTFIX_GROUPLIST = ".grouplist";
    private static final String ENDPOINT_SPLIT_CHAR = ";";
    private static final String IP_PORT_SPLIT_CHAR = ":";

    private FileRegistryServiceImpl() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static FileRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (FileRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new FileRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {

    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {

    }

    @Override
    public void subscribe(String cluster, ConfigChangeListener listener) throws Exception {

    }

    @Override
    public void unsubscribe(String cluster, ConfigChangeListener listener) throws Exception {

    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            String missingDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
            throw new ConfigNotFoundException("%s configuration item is required", missingDataId);
        }
        String endpointStr = getGroupListFromConfig(clusterName);
        String[] endpoints = endpointStr.split(ENDPOINT_SPLIT_CHAR);
        List<InetSocketAddress> inetSocketAddresses = new ArrayList<>();
        for (String endpoint : endpoints) {
            String[] ipAndPort = endpoint.split(IP_PORT_SPLIT_CHAR);
            if (ipAndPort.length != 2) {
                throw new IllegalArgumentException("endpoint format should like ip:port, the invalid endpoint: " + endpoint);
            }
            inetSocketAddresses.add(new InetSocketAddress(ipAndPort[0], Integer.parseInt(ipAndPort[1])));
        }
        return inetSocketAddresses;
    }

    private String getGroupListFromConfig(String clusterName) {
        String oldGrouplistDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + clusterName + POSTFIX_GROUPLIST;
        String endpointStr = CONFIG.getConfig(oldGrouplistDataId);
        if (StringUtils.isNullOrEmpty(endpointStr)) {
            String newGrouplistDataId = PREFIX_SERVICE_ROOT + POSTFIX_GROUPLIST + CONFIG_SPLIT_CHAR + clusterName;
            throw new ConfigNotFoundException("%s or %s configuration item is required", oldGrouplistDataId,
                newGrouplistDataId);
        }
        return endpointStr;
    }

    @Override
    public void close() throws Exception {

    }
}
