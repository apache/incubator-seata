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
package io.seata.server.auth;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.seata.config.ConfigurationChangeEvent;
import io.seata.config.ConfigurationChangeListener;
import io.seata.config.ConfigurationFactory;

public class Blacklist {

    private static final long DEFAULT_CONFIG_TIMEOUT = 5000;

    private static final String IP_CONFIG_SPLIT_CHAR = ";";

    private List<String> ipList = new CopyOnWriteArrayList<>();

    public Blacklist(String blacklistConfig) {
        String ips = ConfigurationFactory.getInstance().getConfig(blacklistConfig);
        if (ips != null) {
            String[] ipArray = ips.split(IP_CONFIG_SPLIT_CHAR);
            Collections.addAll(ipList, ipArray);
        }

        ConfigurationFactory.getInstance().addConfigListener(blacklistConfig, new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                String currentIps = event.getNewValue();
                clear();
                if (currentIps == null) {
                    return;
                }
                String[] currentIpArray = currentIps.split(IP_CONFIG_SPLIT_CHAR);
                Collections.addAll(ipList, currentIpArray);
            }
        });
    }

    public void setIpList(List<String> ipList) {
        this.ipList = ipList;
    }

    public List<String> getIpList() {
        return ipList;
    }

    public void clear() {
        ipList.clear();
    }

    public boolean contains(String address) {
        return ipList.contains(address);
    }
}

