package com.alibaba.fescar.discovery.registry;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaEventListener;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The type Eureka registry service.
 *
 * @Author: rui_849217@163.com
 * @Project: fescar-all
 * @DateTime: 2019 /02/18 16:31
 * @FileName: EurekaRegistryServiceImpl
 * @Description: config center use FILE type at now,
 *  eureka is not implement because eureka don't have related API
 */
public class EurekaRegistryServiceImpl implements RegistryService<EurekaEventListener>{

    private static final String DEFAULT_APPLICATION = "default";
    private static final String PRO_SERVICE_URL_KEY = "serviceUrl";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "eureka";
    private static final String REGISTRY_APPLICATION= "application";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static final String EUREKA_CONFIG_SERVER_URL_KEY = "eureka.serviceUrl.default";

    private static ApplicationInfoManager applicationInfoManager;
    private static CustomEurekaInstanceConfig instanceConfig;
    private static InstanceInfo instanceInfo;
    private static EurekaRegistryServiceImpl instance;
    private static EurekaClient eurekaClient;
    private static Properties eurekaProperties;

    private EurekaRegistryServiceImpl() {}

    public static EurekaRegistryServiceImpl getInstance() {
        if (null == instance) {
            synchronized (EurekaRegistryServiceImpl.class) {
                if (null == instance) {
                    instance = new EurekaRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        validAddress(address);

        if (instanceConfig == null){
            instanceConfig = new CustomEurekaInstanceConfig();
            instanceConfig.setIpAddress(address.getAddress().getHostAddress());
            instanceConfig.setPort(address.getPort());
            instanceConfig.setApplicationName(getApplicationName());
            instanceConfig.setInstanceId(getInstanceId());
        }
        getEurekaClient();
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        validAddress(address);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        getEurekaClient().shutdown();
        close();
    }

    @Override
    public void subscribe(String cluster, EurekaEventListener listener) throws Exception {
        getEurekaClient().registerEventListener(listener);
    }

    @Override
    public void unsubscribe(String cluster, EurekaEventListener listener) throws Exception {
        getEurekaClient().unregisterEventListener(listener);
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        List<InstanceInfo> instances = getEurekaClient().getApplication(key.toUpperCase()).getInstances();
        List<InetSocketAddress> inetSocketAddressList = new ArrayList<>();
        for (InstanceInfo instanceInfo : instances){
            inetSocketAddressList.add(new InetSocketAddress(instanceInfo.getIPAddr(),instanceInfo.getPort()));
        }
        return inetSocketAddressList;
    }

    private static void close() {
        applicationInfoManager = null;
        instanceConfig = null;
        instanceInfo = null;
        eurekaProperties = null;
        eurekaClient = null;
    }

    private static Properties getEurekaProperties() {
        if (eurekaProperties != null) {
            return eurekaProperties;
        }
        eurekaProperties = new Properties();
        eurekaProperties.setProperty("eureka.shouldEnforceRegistrationAtInit", "true");
        String url = FILE_CONFIG.getConfig(getEurekaServerUrlFileKey());
        if (url != null) {
            eurekaProperties.setProperty(EUREKA_CONFIG_SERVER_URL_KEY, url);
        }

        return eurekaProperties;
    }

    private static String getApplicationName() {
        String application = FILE_CONFIG.getConfig(getEurekaApplicationFileKey());
        if (null == application) {
            application = DEFAULT_APPLICATION;
        }
        return application;
    }

    private static EurekaClient getEurekaClient() throws Exception {
        if (eurekaClient == null){
            synchronized (EurekaRegistryServiceImpl.class) {
                if (eurekaClient == null){
                    ConfigurationManager.loadProperties(getEurekaProperties());
                    instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
                    applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
                    eurekaClient = new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig());
                }
            }
        }
        return eurekaClient;
    }

    private void validAddress(InetSocketAddress address) {
        if (null == address.getHostName() || 0 == address.getPort()) {
            throw new IllegalArgumentException("invalid address:" + address);
        }
    }

    private static String getInstanceId() {
        return instanceConfig.getAppname() + "/" + instanceConfig.getIpAddress() + ":" + instanceConfig.getNonSecurePort();
    }

    private static String getEurekaServerUrlFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + PRO_SERVICE_URL_KEY;
    }

    private static String getEurekaApplicationFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + REGISTRY_APPLICATION;
    }
}
