package com.alibaba.fescar.discovery.registry;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import org.apache.commons.lang3.StringUtils;

/**
 * The custom eureka instance config info
 *
 * @Author: rui_849217@163.com
 * @Project: fescar-all
 * @DateTime: 2019 /02/18 16:31
 * @FileName: CustomEurekaInstanceConfig
 * @Description: override MyDataCenterInstanceConfig for set value,
 * eg: instanceId \ipAddress \ applicationName...
 */
public class CustomEurekaInstanceConfig extends MyDataCenterInstanceConfig implements EurekaInstanceConfig {
    private String applicationName;
    private String instanceId;
    private String ipAddress;
    private int port = -1;
    @Override
    public String getInstanceId() {
        if (StringUtils.isBlank(instanceId)){
            return super.getInstanceId();
        }
        return instanceId;
    }


    @Override
    public String getIpAddress() {
        if (StringUtils.isBlank(ipAddress)){
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
        if (StringUtils.isBlank(applicationName)){
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
