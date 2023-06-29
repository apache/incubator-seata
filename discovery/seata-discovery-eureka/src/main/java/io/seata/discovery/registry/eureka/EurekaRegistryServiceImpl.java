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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaEventListener;
import com.netflix.discovery.shared.Application;
import io.seata.common.exception.EurekaRegistryException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.exception.ConfigNotFoundException;
import io.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * The type Eureka registry service.
 *
 * @author rui_849217@163.com
 */
public class EurekaRegistryServiceImpl implements RegistryService<EurekaEventListener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EurekaRegistryServiceImpl.class);

    private static final String DEFAULT_APPLICATION = "default";
    private static final String PRO_SERVICE_URL_KEY = "serviceUrl";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "eureka";
    private static final String CLUSTER = "application";
    private static final String REGISTRY_WEIGHT = "weight";
    private static final String EUREKA_CONFIG_SERVER_URL_KEY = "eureka.serviceUrl.default";
    private static final String EUREKA_CONFIG_REFRESH_KEY = "eureka.client.refresh.interval";
    private static final String EUREKA_CONFIG_SHOULD_REGISTER = "eureka.registration.enabled";
    private static final String EUREKA_CONFIG_METADATA_WEIGHT = "eureka.metadata.weight";
    private static final int EUREKA_REFRESH_INTERVAL = 5;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static final String DEFAULT_WEIGHT = "1";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final ConcurrentMap<String, List<EurekaEventListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Object> CLUSTER_LOCK = new ConcurrentHashMap<>();

    private static volatile ApplicationInfoManager applicationInfoManager;
    private static volatile CustomEurekaInstanceConfig instanceConfig;
    private static volatile EurekaRegistryServiceImpl instance;
    private static volatile EurekaClient eurekaClient;

    private EurekaRegistryServiceImpl() {
    }

    static EurekaRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (EurekaRegistryServiceImpl.class) {
                if (instance == null) {
                    instanceConfig = new CustomEurekaInstanceConfig();
                    instance = new EurekaRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        instanceConfig.setIpAddress(address.getAddress().getHostAddress());
        instanceConfig.setPort(address.getPort());
        instanceConfig.setApplicationName(getApplicationName());
        instanceConfig.setInstanceId(getInstanceId());
        getEurekaClient(true);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        if (eurekaClient == null) {
            return;
        }
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
    }

    @Override
    public void subscribe(String cluster, EurekaEventListener listener) throws Exception {
        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new ArrayList<>())
                .add(listener);
        getEurekaClient(false).registerEventListener(listener);
    }

    @Override
    public void unsubscribe(String cluster, EurekaEventListener listener) throws Exception {
        List<EurekaEventListener> subscribeList = LISTENER_SERVICE_MAP.get(cluster);
        if (subscribeList != null) {
            List<EurekaEventListener> newSubscribeList = subscribeList.stream()
                    .filter(eventListener -> !eventListener.equals(listener))
                    .collect(Collectors.toList());
            LISTENER_SERVICE_MAP.put(cluster, newSubscribeList);
        }
        getEurekaClient(false).unregisterEventListener(listener);
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            String missingDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
            throw new ConfigNotFoundException("%s configuration item is required", missingDataId);
        }
        String clusterUpperName = clusterName.toUpperCase();
        if (!LISTENER_SERVICE_MAP.containsKey(clusterUpperName)) {
            Object lock = CLUSTER_LOCK.computeIfAbsent(clusterUpperName, k -> new Object());
            synchronized (lock) {
                if (!LISTENER_SERVICE_MAP.containsKey(clusterUpperName)) {
                    refreshCluster(clusterUpperName);
                    subscribe(clusterUpperName, event -> {
                        refreshCluster(clusterUpperName);
                    });
                }
            }
        }
        return CLUSTER_ADDRESS_MAP.get(clusterUpperName);
    }

    @Override
    public void close() throws Exception {
        if (eurekaClient != null) {
            eurekaClient.shutdown();
        }
        clean();
    }

    private void refreshCluster(String clusterName) {
        Application application = getEurekaClient(false).getApplication(clusterName);
        if (application == null || CollectionUtils.isEmpty(application.getInstances())) {
            LOGGER.info("refresh cluster success,but cluster empty! cluster name:{}", clusterName);
        } else {
            List<InetSocketAddress> newAddressList = application.getInstances().stream()
                    .filter(instance -> InstanceInfo.InstanceStatus.UP.equals(instance.getStatus()) && instance.getIPAddr() != null && instance.getPort() > 0 && instance.getPort() < 0xFFFF)
                    .map(instance -> new InetSocketAddress(instance.getIPAddr(), instance.getPort()))
                    .collect(Collectors.toList());
            CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
        }
    }

    private Properties getEurekaProperties(boolean needRegister) {
        Properties eurekaProperties = new Properties();
        eurekaProperties.setProperty(EUREKA_CONFIG_REFRESH_KEY, String.valueOf(EUREKA_REFRESH_INTERVAL));

        String url = FILE_CONFIG.getConfig(getEurekaServerUrlFileKey());
        if (StringUtils.isBlank(url)) {
            throw new EurekaRegistryException("eureka server url can not be null!");
        }
        eurekaProperties.setProperty(EUREKA_CONFIG_SERVER_URL_KEY, url);

        String weight = FILE_CONFIG.getConfig(getEurekaInstanceWeightFileKey());
        if (StringUtils.isNotBlank(weight)) {
            eurekaProperties.setProperty(EUREKA_CONFIG_METADATA_WEIGHT, weight);
        } else {
            eurekaProperties.setProperty(EUREKA_CONFIG_METADATA_WEIGHT, DEFAULT_WEIGHT);
        }

        if (!needRegister) {
            eurekaProperties.setProperty(EUREKA_CONFIG_SHOULD_REGISTER, "false");
        }

        return eurekaProperties;
    }

    private String getApplicationName() {
        String application = FILE_CONFIG.getConfig(getEurekaApplicationFileKey());
        if (application == null) {
            application = DEFAULT_APPLICATION;
        }
        return application;
    }

    private EurekaClient getEurekaClient(boolean needRegister) throws EurekaRegistryException {
        if (eurekaClient == null) {
            synchronized (EurekaRegistryServiceImpl.class) {
                try {
                    if (eurekaClient == null) {
                        if (!needRegister) {
                            instanceConfig = new CustomEurekaInstanceConfig();
                        }
                        ConfigurationManager.loadProperties(getEurekaProperties(needRegister));
                        InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
                        applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
                        eurekaClient = new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig());
                    }
                } catch (Exception e) {
                    clean();
                    throw new EurekaRegistryException("register eureka is error!", e);
                }
            }
        }
        return eurekaClient;
    }

    private void clean() {
        eurekaClient = null;
        applicationInfoManager = null;
        instanceConfig = null;
    }

    private String getInstanceId() {
        return String.format("%s:%s:%d", instanceConfig.getIpAddress(), instanceConfig.getAppname(),
                instanceConfig.getNonSecurePort());
    }

    private String getEurekaServerUrlFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_SERVICE_URL_KEY);
    }

    private String getEurekaApplicationFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, CLUSTER);
    }

    private String getEurekaInstanceWeightFileKey() {
        return String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, REGISTRY_TYPE, REGISTRY_WEIGHT);
    }
}
