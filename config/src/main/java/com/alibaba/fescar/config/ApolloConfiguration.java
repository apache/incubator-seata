package com.alibaba.fescar.config;

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.ConfigChangeListener;
import static com.alibaba.fescar.config.ConfigurationKeys.*;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by kl on 2019/2/14.
 * Content :The type Apollo configuration.
 */
public class ApolloConfiguration extends AbstractConfiguration<ConfigChangeListener> {

    private static final String REGISTRY_TYPE = "apollo";
    public static final String APP_ID = "app.id";
    public static final String APOLLO_META = "apollo.meta";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static volatile Config config;

    public ApolloConfiguration() {
        readyApolloConfig();
        if (null == config) {
            config = ConfigService.getAppConfig();
        }
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        return config.getProperty(dataId, defaultValue);
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support putConfig");
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        throw new NotSupportYetException("not support removeConfig");
    }

    @Override
    public void addConfigListener(String dataId, ConfigChangeListener listener) {
        Set<String> strings = new HashSet<>();
        strings.add(dataId);
        config.addChangeListener(listener, strings);
    }

    @Override
    public void removeConfigListener(String dataId, ConfigChangeListener listener) {
        throw new NotSupportYetException("not support removeConfigListener");
    }

    @Override
    public List<ConfigChangeListener> getConfigListeners(String dataId) {
        throw new NotSupportYetException("not support getConfigListeners");
    }

    private void readyApolloConfig(){
        Properties  properties = System.getProperties();
        if(!properties.containsKey(APP_ID)){
            System.setProperty(APP_ID,FILE_CONFIG.getConfig(getApolloAppIdFileKey()));
        }
        if(!properties.containsKey(APOLLO_META)){
            System.setProperty(APOLLO_META,FILE_CONFIG.getConfig(getApolloMetaFileKey()));
        }
    }

    @Override
    public String getTypeName() {
        return REGISTRY_TYPE;
    }
    private static String getApolloMetaFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + APOLLO_META;
    }
    private static String getApolloAppIdFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + APP_ID;
    }
}
