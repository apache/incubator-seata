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
package io.seata.discovery.registry.nacos;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.ConfigurationKeys;
import io.seata.config.exception.ConfigNotFoundException;
import io.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The type Nacos registry service.
 *
 * @author slievrly
 * @author xingfudeshi@gmail.com
 */
public class NacosRegistryServiceImpl implements RegistryService<EventListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosRegistryServiceImpl.class);
    private static final String DEFAULT_NAMESPACE = "";
    private static final String DEFAULT_CLUSTER = "default";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    private static final String DEFAULT_APPLICATION = "seata-server";
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String PRO_NAMESPACE_KEY = "namespace";
    private static final String REGISTRY_TYPE = "nacos";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final String PRO_APPLICATION_KEY = "application";
    private static final String PRO_CLIENT_APPLICATION = "clientApplication";
    private static final String PRO_GROUP_KEY = "group";
    private static final String USER_NAME = "username";
    private static final String PASSWORD = "password";
    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRET_KEY = "secretKey";
    private static final String SLB_PATTERN = "slbPattern";
    private static final String CONTEXT_PATH = "contextPath";
    private static final String USE_PARSE_RULE = "false";
    private static final String PUBLIC_NAMING_ADDRESS_PREFIX = "public_";
    private static final String PUBLIC_NAMING_SERVICE_META_IP_KEY = "publicIp";
    private static final String PUBLIC_NAMING_SERVICE_META_PORT_KEY = "publicPort";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile NamingService naming;
    private static final ConcurrentMap<String, List<EventListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static volatile NacosRegistryServiceImpl instance;
    private static volatile NamingMaintainService namingMaintain;
    private static final Object LOCK_OBJ = new Object();
    private static final Pattern DEFAULT_SLB_REGISTRY_PATTERN = Pattern.compile("(?!.*internal)(?=.*seata).*mse.aliyuncs.com");
    private static volatile Boolean useSLBWay;

    private NacosRegistryServiceImpl() {
        String configForNacosSLB = FILE_CONFIG.getConfig(getNacosUrlPatternOfSLB());
        Pattern patternOfNacosRegistryForSLB = StringUtils.isBlank(configForNacosSLB)
                ? DEFAULT_SLB_REGISTRY_PATTERN
                : Pattern.compile(configForNacosSLB);
        useSLBWay = patternOfNacosRegistryForSLB.matcher(getNamingProperties().getProperty(PRO_SERVER_ADDR_KEY)).matches();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static NacosRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (NacosRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new NacosRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        getNamingInstance().registerInstance(getServiceName(), getServiceGroup(), address.getAddress().getHostAddress(), address.getPort(), getClusterName());
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        getNamingInstance().deregisterInstance(getServiceName(), getServiceGroup(), address.getAddress().getHostAddress(), address.getPort(), getClusterName());
    }

    @Override
    public void subscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new ArrayList<>())
                .add(listener);
        getNamingInstance().subscribe(getServiceName(), getServiceGroup(), clusters, listener);
    }

    @Override
    public void unsubscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        List<EventListener> subscribeList = LISTENER_SERVICE_MAP.get(cluster);
        if (subscribeList != null) {
            List<EventListener> newSubscribeList = subscribeList.stream()
                    .filter(eventListener -> !eventListener.equals(listener))
                    .collect(Collectors.toList());
            LISTENER_SERVICE_MAP.put(cluster, newSubscribeList);
        }
        getNamingInstance().unsubscribe(getServiceName(), getServiceGroup(), clusters, listener);
    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            String missingDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
            throw new ConfigNotFoundException("%s configuration item is required", missingDataId);
        }
        if (useSLBWay) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("look up service address of SLB by nacos");
            }
            if (!CLUSTER_ADDRESS_MAP.containsKey(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName)) {
                Service service = getNamingMaintainInstance().queryService(DEFAULT_APPLICATION, clusterName);
                String pubnetIp = service.getMetadata().get(PUBLIC_NAMING_SERVICE_META_IP_KEY);
                String pubnetPort = service.getMetadata().get(PUBLIC_NAMING_SERVICE_META_PORT_KEY);
                if (StringUtils.isBlank(pubnetIp) || StringUtils.isBlank(pubnetPort)) {
                    throw new Exception("cannot find service address from nacos naming mata-data");
                }
                InetSocketAddress publicAddress = new InetSocketAddress(pubnetIp,
                        Integer.valueOf(pubnetPort));
                List<InetSocketAddress> publicAddressList = Arrays.asList(publicAddress);
                CLUSTER_ADDRESS_MAP.put(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName, publicAddressList);
                return publicAddressList;
            }
            return CLUSTER_ADDRESS_MAP.get(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName);
        }
        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            synchronized (LOCK_OBJ) {
                if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
                    List<String> clusters = new ArrayList<>();
                    clusters.add(clusterName);
                    List<Instance> firstAllInstances = getNamingInstance().getAllInstances(getServiceName(), getServiceGroup(), clusters);
                    if (null != firstAllInstances) {
                        List<InetSocketAddress> newAddressList = firstAllInstances.stream()
                                .filter(eachInstance -> eachInstance.isEnabled() && eachInstance.isHealthy())
                                .map(eachInstance -> new InetSocketAddress(eachInstance.getIp(), eachInstance.getPort()))
                                .collect(Collectors.toList());
                        CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
                    }
                    subscribe(clusterName, event -> {
                        List<Instance> instances = ((NamingEvent) event).getInstances();
                        if (CollectionUtils.isEmpty(instances) && null != CLUSTER_ADDRESS_MAP.get(clusterName)) {
                            LOGGER.info("receive empty server list,cluster:{}", clusterName);
                        } else {
                            List<InetSocketAddress> newAddressList = instances.stream()
                                    .filter(eachInstance -> eachInstance.isEnabled() && eachInstance.isHealthy())
                                    .map(eachInstance -> new InetSocketAddress(eachInstance.getIp(), eachInstance.getPort()))
                                    .collect(Collectors.toList());
                            CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
                        }
                    });
                }
            }
        }
        return CLUSTER_ADDRESS_MAP.get(clusterName);
    }

    @Override
    public void close() throws Exception {

    }

    /**
     * Gets naming instance.
     *
     * @return the naming instance
     * @throws Exception the exception
     */
    public static NamingService getNamingInstance() throws Exception {
        if (naming == null) {
            synchronized (NacosRegistryServiceImpl.class) {
                if (naming == null) {
                    naming = NacosFactory.createNamingService(getNamingProperties());
                }
            }
        }
        return naming;
    }

    public static NamingMaintainService getNamingMaintainInstance() throws Exception {
        if (namingMaintain == null) {
            synchronized (NacosRegistryServiceImpl.class) {
                if (namingMaintain == null) {
                    namingMaintain = NacosFactory.createMaintainService(getNamingProperties());
                }
            }
        }
        return namingMaintain;
    }

    private static Properties getNamingProperties() {
        Properties properties = new Properties();
        properties.setProperty(ConfigurationKeys.IS_USE_CLOUD_NAMESPACE_PARSING, USE_PARSE_RULE);
        properties.setProperty(ConfigurationKeys.IS_USE_ENDPOINT_PARSING_RULE, USE_PARSE_RULE);
        if (System.getProperty(PRO_SERVER_ADDR_KEY) != null) {
            properties.setProperty(PRO_SERVER_ADDR_KEY, System.getProperty(PRO_SERVER_ADDR_KEY));
        } else {
            String address = FILE_CONFIG.getConfig(getNacosAddrFileKey());
            if (address != null) {
                properties.setProperty(PRO_SERVER_ADDR_KEY, address);
            }
        }
        if (System.getProperty(PRO_NAMESPACE_KEY) != null) {
            properties.setProperty(PRO_NAMESPACE_KEY, System.getProperty(PRO_NAMESPACE_KEY));
        } else {
            String namespace = FILE_CONFIG.getConfig(getNacosNameSpaceFileKey());
            if (namespace == null) {
                namespace = DEFAULT_NAMESPACE;
            }
            properties.setProperty(PRO_NAMESPACE_KEY, namespace);
        }
        String userName = StringUtils.isNotBlank(System.getProperty(USER_NAME)) ? System.getProperty(USER_NAME) : FILE_CONFIG.getConfig(getNacosUserName());
        if (StringUtils.isNotBlank(userName)) {
            String password = StringUtils.isNotBlank(System.getProperty(PASSWORD)) ? System.getProperty(PASSWORD) : FILE_CONFIG.getConfig(getNacosPassword());
            if (StringUtils.isNotBlank(password)) {
                properties.setProperty(USER_NAME, userName);
                properties.setProperty(PASSWORD, password);
            }
        } else {
            String accessKey = StringUtils.isNotBlank(System.getProperty(ACCESS_KEY)) ? System.getProperty(ACCESS_KEY) : FILE_CONFIG.getConfig(getNacosAccessKey());
            if (StringUtils.isNotBlank(accessKey)) {
                String secretKey = StringUtils.isNotBlank(System.getProperty(SECRET_KEY)) ? System.getProperty(SECRET_KEY) : FILE_CONFIG.getConfig(getNacosSecretKey());
                if (StringUtils.isNotBlank(secretKey)) {
                    properties.put(ACCESS_KEY, accessKey);
                    properties.put(SECRET_KEY, secretKey);
                    LOGGER.info("Nacos check auth with ak/sk.");
                }
            }
        }
        String contextPath = StringUtils.isNotBlank(System.getProperty(CONTEXT_PATH)) ? System.getProperty(CONTEXT_PATH) : FILE_CONFIG.getConfig(getNacosContextPathKey());
        if (StringUtils.isNotBlank(contextPath)) {
            properties.setProperty(CONTEXT_PATH, contextPath);
        }
        return properties;
    }

    private static String getClusterName() {
        return FILE_CONFIG.getConfig(getNacosClusterFileKey(), DEFAULT_CLUSTER);
    }

    private static String getServiceName() {
        return FILE_CONFIG.getConfig(getNacosApplicationFileKey(), DEFAULT_APPLICATION);
    }

    private static String getServiceGroup() {
        return FILE_CONFIG.getConfig(getNacosApplicationGroupKey(), DEFAULT_GROUP);
    }

    private static String getNacosAddrFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_SERVER_ADDR_KEY);
    }

    private static String getNacosNameSpaceFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_NAMESPACE_KEY);
    }

    private static String getNacosClusterFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, REGISTRY_CLUSTER);
    }

    private static String getNacosApplicationFileKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_APPLICATION_KEY);
    }

    private static String getNacosApplicationGroupKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_GROUP_KEY);
    }

    private static String getNacosUserName() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, USER_NAME);
    }

    private static String getNacosPassword() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PASSWORD);
    }

    public static String getNacosAccessKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, ACCESS_KEY);
    }

    public static String getNacosSecretKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, SECRET_KEY);
    }

    public static String getClientApplication() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_CLIENT_APPLICATION);
    }

    private static String getNacosUrlPatternOfSLB() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, SLB_PATTERN);
    }

    private static String getNacosContextPathKey() {
        return String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, CONTEXT_PATH);
    }

}
