package com.alibaba.fescar.discovery.registry;

import com.alibaba.fescar.common.exception.EurekaRegistryException;
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
import com.netflix.discovery.shared.Application;
import org.apache.commons.lang3.StringUtils;
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
    private static final String REGISTRY_WEIGHT = "weight";
    private static final String EUREKA_CONFIG_SERVER_URL_KEY = "eureka.serviceUrl.default";
    private static final String EUREKA_CONFIG_REFRESH_KEY = "eureka.client.refresh.interval";
    private static final String EUREKA_CONFIG_METADATA_WEIGHT = "eureka.metadata.weight";
    private static final int EUREKA_REFRESH_INTERVAL = 5;
    private static final String DEFAULT_WEIGHT = "1";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;

    private static ApplicationInfoManager applicationInfoManager;
    private static CustomEurekaInstanceConfig instanceConfig;
    private static EurekaRegistryServiceImpl instance;
    private static EurekaClient eurekaClient;

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
        if (eurekaClient == null) {
            return;
        }
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        eurekaClient.shutdown();
        close();
    }

    @Override
    public void subscribe(String cluster, EurekaEventListener listener) throws Exception {
        assertEureka();
        eurekaClient.registerEventListener(listener);
    }

    @Override
    public void unsubscribe(String cluster, EurekaEventListener listener) throws Exception {
        assertEureka();
        eurekaClient.unregisterEventListener(listener);
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        assertEureka();
        List<InetSocketAddress> inetSocketAddressList = new ArrayList<>();

        Application application = eurekaClient.getApplication(key.toUpperCase());
        if (application == null){
            return inetSocketAddressList;
        }

        List<InstanceInfo> instances = application.getInstances();
        if (instances == null){
            return inetSocketAddressList;
        }

        for (InstanceInfo instanceInfo : instances){
            inetSocketAddressList.add(new InetSocketAddress(instanceInfo.getIPAddr(),instanceInfo.getPort()));
        }

        return inetSocketAddressList;
    }

    private static void close() {
        applicationInfoManager = null;
        instanceConfig = null;
        eurekaClient = null;
    }

    private static Properties getEurekaProperties() {
        Properties eurekaProperties = new Properties();
        eurekaProperties.setProperty(EUREKA_CONFIG_REFRESH_KEY,String.valueOf(EUREKA_REFRESH_INTERVAL));

        String url = FILE_CONFIG.getConfig(getEurekaServerUrlFileKey());
        if (StringUtils.isBlank(url)) {
            throw new EurekaRegistryException("eureka server url can not be null!");
        }
        eurekaProperties.setProperty(EUREKA_CONFIG_SERVER_URL_KEY, url);

        String weight = FILE_CONFIG.getConfig(getEurekaInstanceWeightFileKey());
        if (StringUtils.isNotBlank(weight)){
            eurekaProperties.setProperty(EUREKA_CONFIG_METADATA_WEIGHT, weight);
        } else {
            eurekaProperties.setProperty(EUREKA_CONFIG_METADATA_WEIGHT, DEFAULT_WEIGHT);
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
                try {
                    if (eurekaClient == null){
                        ConfigurationManager.loadProperties(getEurekaProperties());
                        InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
                        applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
                        eurekaClient = new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig());
                    }
                } catch (Exception e) {
                    close();
                    throw new EurekaRegistryException("register eureka is error!",e);
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

    private static void assertEureka(){
        if (eurekaClient == null) {
            throw new EurekaRegistryException("eureka client is not register,do this operation had to be register!");
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

    private static String getEurekaInstanceWeightFileKey() {
         return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                 + REGISTRY_WEIGHT;
    }
}
