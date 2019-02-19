package com.alibaba.fescar.config;

import com.alibaba.nacos.api.config.listener.Listener;

import java.util.List;

/**
 * todo don't implement at now
 */
public class EurekaConfiguration extends AbstractConfiguration<Listener> {
    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        return null;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        return false;
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        return false;
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        return false;
    }

    @Override
    public void addConfigListener(String dataId, Listener listener) {

    }

    @Override
    public void removeConfigListener(String dataId, Listener listener) {

    }

    @Override
    public List<Listener> getConfigListeners(String dataId) {
        return null;
    }

    @Override
    public String getTypeName() {
        return null;
    }
}
