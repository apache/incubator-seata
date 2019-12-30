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
package io.seata.discovery.registry.eureka;

import io.seata.common.util.StringUtils;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.MyDataCenterInstanceConfig;

/**
 * @author: rui_849217@163.com
 * override MyDataCenterInstanceConfig for set value,
 * eg: instanceId \ipAddress \ applicationName...
 */
public class CustomEurekaInstanceConfig extends MyDataCenterInstanceConfig implements EurekaInstanceConfig {
    private String applicationName;
    private String instanceId;
    private String ipAddress;
    private int port = -1;

    @Override
    public String getInstanceId() {
        if (StringUtils.isBlank(instanceId)) {
            return super.getInstanceId();
        }
        return instanceId;
    }

    @Override
    public String getIpAddress() {
        if (StringUtils.isBlank(ipAddress)) {
            return super.getIpAddress();
        }
        return ipAddress;
    }

    @Override
    public int getNonSecurePort() {
        if (port == -1) {
            return super.getNonSecurePort();
        }
        return port;
    }

    @Override
    public String getAppname() {
        if (StringUtils.isBlank(applicationName)) {
            return super.getAppname();
        }
        return applicationName;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
