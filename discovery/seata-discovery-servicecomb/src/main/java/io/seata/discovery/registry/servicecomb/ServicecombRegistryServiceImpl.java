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

package io.seata.discovery.registry.servicecomb;

import io.seata.common.ConfigurationKeys;
import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryService;
import io.seata.discovery.registry.servicecomb.client.EventManager;
import io.seata.discovery.registry.servicecomb.client.auth.AuthHeaderProviders;
import org.apache.commons.lang3.StringUtils;
import org.apache.servicecomb.http.client.common.HttpConfiguration;
import org.apache.servicecomb.service.center.client.AddressManager;
import org.apache.servicecomb.service.center.client.ServiceCenterClient;
import org.apache.servicecomb.service.center.client.ServiceCenterDiscovery;
import org.apache.servicecomb.service.center.client.ServiceCenterDiscovery.SubscriptionKey;
import org.apache.servicecomb.service.center.client.ServiceCenterRegistration;
import org.apache.servicecomb.service.center.client.ServiceCenterWatch;
import org.apache.servicecomb.service.center.client.model.Framework;
import org.apache.servicecomb.service.center.client.model.HealthCheck;
import org.apache.servicecomb.service.center.client.model.HealthCheckMode;
import org.apache.servicecomb.service.center.client.model.Microservice;
import org.apache.servicecomb.service.center.client.model.MicroserviceInstance;
import org.apache.servicecomb.service.center.client.model.MicroserviceInstanceStatus;
import org.apache.servicecomb.service.center.client.model.RegisteredMicroserviceResponse;
import org.apache.servicecomb.service.center.client.model.ServiceCenterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type Servicecomb registry service.
 *
 * @author zhaozhongwei22@163.com
 */
public class ServicecombRegistryServiceImpl implements RegistryService<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicecombRegistryServiceImpl.class);

    private static final String FRAMEWORK_NAME = "SEATA-DISCOVERY-SERVICECOMB";
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private static volatile ServicecombRegistryServiceImpl instance;

    private final Microservice microservice;

    private final MicroserviceInstance microserviceInstance;

    private final ServiceCenterClient serviceCenterClient;

    private final ServiceCenterRegistration serviceCenterRegistration;

    private final ServiceCenterDiscovery serviceCenterDiscovery;

    private final ServiceCenterWatch serviceCenterWatch;

    private final Set<String> clusterNameSet = new HashSet<>(8);

    private ServicecombRegistryServiceImpl() {
        microservice = createMicroservice();
        microserviceInstance = createMicroserviceInstance();
        serviceCenterClient = createServiceCenterClient();
        serviceCenterRegistration = createServiceCenterRegistration();
        serviceCenterDiscovery = createServiceCenterDiscovery();
        serviceCenterWatch = createServiceCenterWatch();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static ServicecombRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (ServicecombRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new ServicecombRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);

        List<String> endPoints = new ArrayList<>();
        endPoints.add(getEndPoint(address));
        microserviceInstance.setEndpoints(endPoints);

        serviceCenterRegistration.startRegistration();
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        serviceCenterRegistration.stop();
        if (!StringUtils.isEmpty(microserviceInstance.getInstanceId())) {
            try {
                serviceCenterClient.deleteMicroserviceInstance(microservice.getServiceId(),
                    microserviceInstance.getInstanceId());
            } catch (Exception e) {
                LOGGER.error("delete microservice failed. ", e);
            }
        }
    }

    @Override
    public void subscribe(String cluster, Object listener) throws Exception {}

    @Override
    public void unsubscribe(String cluster, Object listener) throws Exception {}

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            return null;
        }
        fetchMicroserviceServiceId();
        if (clusterNameSet.isEmpty()) {
            // startDiscovery will check if already started, can call several times
            serviceCenterDiscovery.startDiscovery();

            if (SeataServicecombKeys.TRUE
                .equals(FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_REGISTRY_WATCH, SeataServicecombKeys.FALSE))) {
                serviceCenterWatch.startWatch(FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_SERVICE_PROJECT),
                    microservice.getServiceId());
            }
        }
        String appId = microservice.getAppId();
        String serviceId = clusterName;
        int idxAt = clusterName.indexOf(SeataServicecombKeys.APP_SERVICE_SEPRATOR);
        if (idxAt != -1) {
            appId = clusterName.substring(0, idxAt);
            serviceId = clusterName.substring(idxAt + 1);
        }

        SubscriptionKey subscriptionKey = parseMicroserviceName(appId, serviceId);

        if (!clusterNameSet.contains(clusterName)) {
            serviceCenterDiscovery.registerIfNotPresent(subscriptionKey);
            clusterNameSet.add(clusterName);
        }
        List<MicroserviceInstance> instances = serviceCenterDiscovery.getInstanceCache(subscriptionKey);

        if (instances == null) {
            return Collections.emptyList();
        }
        return instances.stream().filter(instance -> !MicroserviceInstanceStatus.DOWN.equals(instance.getStatus()))
            .map(instance -> {
                try {
                    URI uri = new URI(instance.getEndpoints().get(0));
                    InetSocketAddress address = new InetSocketAddress(uri.getHost(), uri.getPort());
                    return address;
                } catch (Exception e) {
                    return null;
                }
            }).collect(Collectors.toList());
    }

    @Override
    public void close() throws Exception {
        serviceCenterDiscovery.stop();
        serviceCenterWatch.stop();
    }

    private Microservice createMicroservice() {
        Microservice microservice = new Microservice();
        microservice.setAppId(
            FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_SERVICE_APPLICATION, SeataServicecombKeys.DEFAULT));
        microservice
            .setServiceName(FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_SERVICE_NAME, SeataServicecombKeys.DEFAULT));
        microservice.setVersion(
            FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_SERVICE_VERSION, SeataServicecombKeys.DEFAULT_VERSION));
        microservice.setEnvironment(
            FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_SERVICE_ENVIRONMENT, SeataServicecombKeys.EMPTY));
        Framework framework = new Framework();
        framework.setName(FRAMEWORK_NAME);
        StringBuilder version = new StringBuilder();
        version.append(FRAMEWORK_NAME.toLowerCase(Locale.ROOT)).append(SeataServicecombKeys.COLON);
        if (StringUtils.isEmpty(ServicecombRegistryServiceImpl.class.getPackage().getImplementationVersion())) {
            version.append(ServicecombRegistryServiceImpl.class.getPackage().getImplementationVersion());
        } else {
            version.append(SeataServicecombKeys.DEFAULT_VERSION);
        }
        version.append(SeataServicecombKeys.SEMICOLON);
        framework.setVersion(version.toString());
        microservice.setFramework(framework);
        if (Boolean.parseBoolean(
            FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_SERVICE_ALLOW_CROSS_APP_KEY, SeataServicecombKeys.TRUE))) {
            microservice.setAlias(
                microservice.getAppId() + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + microservice.getServiceName());
            microservice.getProperties().put(SeataServicecombKeys.CONFIG_ALLOW_CROSS_APP_KEY,
                SeataServicecombKeys.TRUE);
        }
        return microservice;
    }

    private MicroserviceInstance createMicroserviceInstance() {
        MicroserviceInstance instance = new MicroserviceInstance();
        instance.setStatus(MicroserviceInstanceStatus
            .valueOf(FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_INSTANCE_ENVIRONMENT, SeataServicecombKeys.UP)));
        HealthCheck healthCheck = new HealthCheck();
        healthCheck.setMode(HealthCheckMode.pull);
        healthCheck
            .setInterval(Integer.parseInt(FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_INSTANCE_HEALTH_CHECK_INTERVAL,
                SeataServicecombKeys.DEFAULT_INSTANCE_HEALTH_CHECK_INTERVAL)));
        healthCheck
            .setTimes(Integer.parseInt(FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_INSTANCE_HEALTH_CHECK_TIMES,
                SeataServicecombKeys.DEFAULT_INSTANCE_HEALTH_CHECK_TIMES)));
        instance.setHealthCheck(healthCheck);
        try {
            instance.setHostName(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            // ignore
        }
        return instance;
    }

    private ServiceCenterClient createServiceCenterClient() {
        AddressManager addressManager = createAddressManager();
        HttpConfiguration.SSLProperties sslProperties = AuthHeaderProviders.createSslProperties(FILE_CONFIG);
        return new ServiceCenterClient(addressManager, sslProperties,
            AuthHeaderProviders.getRequestAuthHeaderProvider(), SeataServicecombKeys.DEFAULT, null);
    }

    private AddressManager createAddressManager() {
        String address =
            FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_REGISTRY_ADDRESS, SeataServicecombKeys.DEFAULT_REGISTRY_URL);
        String project = FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_SERVICE_PROJECT, SeataServicecombKeys.DEFAULT);
        LOGGER.info("Using service center, address={}.", address);
        return new AddressManager(project, Arrays.asList(address.split(SeataServicecombKeys.COMMA)),
            EventManager.getEventBus());
    }

    private ServiceCenterRegistration createServiceCenterRegistration() {
        ServiceCenterRegistration serviceCenterRegistration = new ServiceCenterRegistration(serviceCenterClient,
            new ServiceCenterConfiguration(), EventManager.getEventBus());
        serviceCenterRegistration.setMicroservice(microservice);
        serviceCenterRegistration.setMicroserviceInstance(microserviceInstance);
        serviceCenterRegistration.setHeartBeatInterval(microserviceInstance.getHealthCheck().getInterval());
        return serviceCenterRegistration;
    }

    private ServiceCenterDiscovery createServiceCenterDiscovery() {
        ServiceCenterDiscovery serviceCenterDiscovery =
            new ServiceCenterDiscovery(serviceCenterClient, EventManager.getEventBus());
        serviceCenterDiscovery
            .setPollInterval(Integer.parseInt(FILE_CONFIG.getConfig(SeataServicecombKeys.KEY_INSTANCE_PULL_INTERVAL,
                SeataServicecombKeys.DEFAULT_INSTANCE_PULL_INTERVAL)));
        return serviceCenterDiscovery;
    }

    private ServiceCenterWatch createServiceCenterWatch() {
        ServiceCenterWatch watch = new ServiceCenterWatch(createAddressManager(),
            AuthHeaderProviders.createSslProperties(FILE_CONFIG), AuthHeaderProviders.getRequestAuthHeaderProvider(),
            SeataServicecombKeys.DEFAULT, Collections.EMPTY_MAP, EventManager.getEventBus());
        return watch;
    }

    private String getEndPoint(InetSocketAddress address) throws Exception {
        // URI uri = new URI("seata", null, address.getAddress().getHostAddress(), address.getPort(),
        // "/", null, null);
        // return uri.toString();

        return SeataServicecombKeys.REST_PROTOCOL + address.getAddress().getHostAddress() + SeataServicecombKeys.COLON
            + address.getPort();
    }

    private SubscriptionKey parseMicroserviceName(String appId, String serviceId) {
        return new SubscriptionKey(appId, serviceId);
    }

    private void fetchMicroserviceServiceId() {
        if (StringUtils.isEmpty(microservice.getServiceId())) {
            RegisteredMicroserviceResponse response = serviceCenterClient.queryServiceId(microservice);
            if (response != null) {
                microservice.setServiceId(response.getServiceId());
                serviceCenterDiscovery.updateMyselfServiceId(microservice.getServiceId());
            }
        }
    }
}
