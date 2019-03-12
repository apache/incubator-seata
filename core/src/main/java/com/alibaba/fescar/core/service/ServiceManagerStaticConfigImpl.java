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

import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;

import static com.alibaba.fescar.common.exception.FrameworkErrorCode.InvalidConfiguration;
import static com.alibaba.fescar.core.service.ConfigurationKeys.SERVER_NODE_SPLIT_CHAR;

/**
 * The type Service manager static config.
 */
public class ServiceManagerStaticConfigImpl implements ServiceManager {

    private Configuration configuration = ConfigurationFactory.getInstance();

    @Override
    public void register(String txServiceGroup, String address) {
        throw new NotSupportYetException("Dynamic registry");

    }

    @Override
    public void unregister(String txServiceGroup, String address) {
        throw new NotSupportYetException("Dynamic un-registry");

    }

    @Override
    public void watch(String txServiceGroup, AddressWatcher watcher) {
        throw new NotSupportYetException("Watch");

    }

    private String[] serverAddresses = null;

    @Override
    public String[] lookup(String txServiceGroup) {
        if (serverAddresses != null) {
            return serverAddresses;
        }
        String rGroup = configuration.getConfig(ConfigurationKeys.SERVICE_GROUP_MAPPING_PREFIX + txServiceGroup);
        if (rGroup == null) {
            throw new FrameworkException(InvalidConfiguration);
        }
        String rGroupDataId = ConfigurationKeys.SERVICE_PREFIX + rGroup + ConfigurationKeys.GROUPLIST_POSTFIX;
        String serverListConfig = configuration.getConfig(rGroupDataId);
        if (StringUtils.isNullOrEmpty(serverListConfig)) {
            throw new FrameworkException(InvalidConfiguration);
        }
        serverAddresses = serverListConfig.split(SERVER_NODE_SPLIT_CHAR);
        if (serverAddresses == null || serverAddresses.length == 0) {
            throw new FrameworkException(InvalidConfiguration);
        }
        return serverAddresses;

    }
}
