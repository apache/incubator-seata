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
package io.seata.discovery.registry.polaris.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.JacksonUtils;
import io.seata.common.util.StringUtils;
import io.seata.discovery.registry.polaris.PolarisListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.discovery.registry.polaris.PolarisRegistryServiceImpl.DEFAULT_CLUSTER;
import static io.seata.discovery.registry.polaris.client.PolarisNamingClient.PolarisRegistryRequests.DEREGISTER_INSTANCE;
import static io.seata.discovery.registry.polaris.client.PolarisNamingClient.PolarisRegistryRequests.GET_ALL_INSTANCES;
import static io.seata.discovery.registry.polaris.client.PolarisNamingClient.PolarisRegistryRequests.REGISTER_INSTANCE;
import static io.seata.discovery.registry.polaris.client.PolarisNamingClient.Request.ACCESS_TOKEN_HEADER;
import static io.seata.discovery.registry.polaris.client.PolarisNamingClient.Request.RESPONSE_OK;
import static io.seata.discovery.registry.polaris.client.SimpleHttpRequest.CHARSET_UTF8;
import static io.seata.discovery.registry.polaris.client.SimpleHttpRequest.CONTENT_TYPE_JSON;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Polaris Registry Center Operation Client.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-23
 */
public final class PolarisNamingClient {

    /**
     * Logger Instance.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PolarisNamingClient.class);

    /**
     * Singleton Naming Client Instance.
     */
    private static volatile PolarisNamingClient instance;

    /**
     * Polaris Naming Client Config Properties.
     */
    private static PolarisNamingProperties properties;

    /**
     * Get or create new {@link PolarisNamingClient} instance with {@link PolarisNamingClient}.
     *
     * @param properties {@link PolarisNamingClient} build config properties instance of {@link PolarisNamingProperties}
     * @return instance of {@link PolarisNamingClient}
     */
    public static PolarisNamingClient getClient(PolarisNamingProperties properties) {
        if (instance == null) {
            synchronized (PolarisNamingClient.class) {
                if (instance == null) {
                    instance = new PolarisNamingClient(properties);
                }
            }
        }
        return instance;
    }

    private PolarisNamingClient(PolarisNamingProperties properties) {
        PolarisNamingClient.properties = properties;
    }

    // ~~ Core Registry Operations

    /**
     * register an instance to service.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @param ip          instance ip
     * @param port        instance port
     * @throws PolarisNamingException polaris exception
     */
    public void registerInstance(String namespace, String serviceName, String ip, int port) throws PolarisNamingException {
        this.registerInstance(namespace, serviceName, ip, port, DEFAULT_CLUSTER);
    }

    /**
     * register a instance to service.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @param ip          instance ip
     * @param port        instance port
     * @param cluster cluster of service
     * @throws PolarisNamingException polaris exception
     */
    public void registerInstance(String namespace, String serviceName, String ip, int port, String cluster) throws PolarisNamingException {
        try{
            RegistryRequest registryRequest = new RegistryRequest();
            RegistryRequest.InstanceInfo instance = new RegistryRequest.InstanceInfo();
            instance.setService(serviceName);
            instance.setNamespace(namespace);
            instance.setHost(ip);
            instance.setPort(port);
            registryRequest.addInstance(instance);
            RegistryCommonResponse response = (RegistryCommonResponse) REGISTER_INSTANCE.execute(registryRequest);

            if (response != null && response.isOk()) {
                LOGGER.info("[Polaris-Registry] service register succeed .");

                // TODO upgrade local storage.

            }
        } catch (Exception e) {
            throw new PolarisNamingException("polaris service register failed", e);
        }
    }

    /**
     * deregister an instance to service.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @param ip          instance ip
     * @param port        instance port
     * @throws PolarisNamingException polaris exception
     */
    public void deregisterInstance(String namespace, String serviceName, String ip, int port) throws PolarisNamingException {
        deregisterInstance(namespace, serviceName, ip, port, DEFAULT_CLUSTER);
    }

    /**
     * deregister an instance to service.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @param ip          instance ip
     * @param port        instance port
     * @param cluster cluster of service
     * @throws PolarisNamingException polaris exception
     */
    public void deregisterInstance(String namespace, String serviceName, String ip, int port, String cluster) throws PolarisNamingException {
        try{
            DeregistryRequest deregistryRequest = new DeregistryRequest();
            List<PolarisInstance> instances = new ArrayList<>();
            // TODO fetch instances from local storage.


            for (PolarisInstance polarisInstance : instances) {
                deregistryRequest.addInstance(polarisInstance.getServiceId());
            }

            RegistryCommonResponse response = (RegistryCommonResponse) DEREGISTER_INSTANCE.execute(deregistryRequest);
            if (response != null && response.isOk()) {
                LOGGER.info("[Polaris-Registry] service de-register succeed .");

                // TODO upgrade local storage.

            }
        } catch (Exception e) {
            throw new PolarisNamingException("polaris service de-register failed", e);
        }
    }

    /**
     * get all instances of a service.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @return A list of instance
     * @throws PolarisNamingException polaris exception
     */
    public List<PolarisInstance> getAllInstances(String namespace, String serviceName) throws PolarisNamingException {
        return getAllInstances(namespace, serviceName, DEFAULT_CLUSTER);
    }

    /**
     * get all instances of a service.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @param cluster cluster of service
     * @return A list of instance
     * @throws PolarisNamingException polaris exception
     */
    public List<PolarisInstance> getAllInstances(String namespace, String serviceName, String cluster) throws PolarisNamingException {

        List<PolarisInstance> ret = new ArrayList<>();
        try{
            GetAllServiceInstancesRequest.ServiceMetadata metadata = new GetAllServiceInstancesRequest.ServiceMetadata(serviceName, namespace);
            GetAllServiceInstancesRequest getAllServiceInstancesRequest = new GetAllServiceInstancesRequest(metadata);
            GetAllServiceInstancesResponse response = (GetAllServiceInstancesResponse) GET_ALL_INSTANCES.execute(getAllServiceInstancesRequest);

            if (response != null && response.isOk()) {
                LOGGER.info("[Polaris-Registry] get all service instances succeed .");

                List<GetAllServiceInstancesResponse.InstanceInfo> instances = response.getInstances();

                if (CollectionUtils.isNotEmpty(instances)) {
                    ret.addAll(instances.stream().filter(GetAllServiceInstancesResponse.InstanceInfo::isHealthy)
                        .map(eachInstance -> new PolarisInstance()
                            .setServiceId(eachInstance.getId())
                            .setServiceName(eachInstance.getService())
                            .setHost(eachInstance.getHost())
                            .setPort(eachInstance.getPort())
                            .setHealthy(eachInstance.isHealthy())
                            .setCluster(eachInstance.getCluster())
                            .setEnableHealthCheck(eachInstance.isEnableHealthCheck())
                            .setHealthCheckPeriod(eachInstance.getHealthCheck().getHeartbeat().getTtl())
                            .setMetadata(eachInstance.getMetadata())
                            .setNamespace(eachInstance.getNamespace())
                            .setRevision(eachInstance.getRevision())
                        )
                        .collect(Collectors.toList()));
                }
            }
        } catch (Exception e) {
            throw new PolarisNamingException("polaris get all service instances failed", e);
        }
        return ret;
    }

    /**
     * Get qualified instances of service.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @param cluster cluster of service
     * @param healthy     a flag to indicate returning healthy or unhealthy instances
     * @return A qualified list of instance
     * @throws PolarisNamingException polaris exception
     */
    public List<PolarisInstance> selectInstances(String namespace, String serviceName, String cluster, boolean healthy) throws PolarisNamingException {
        List<PolarisInstance> ret = new ArrayList<>();
        try{
            List<PolarisInstance> instances = getAllInstances(namespace, serviceName, cluster);
            if (CollectionUtils.isNotEmpty(instances)) {
                ret.addAll(instances.stream().filter(PolarisInstance::isHealthy).collect(Collectors.toList()));
            }
        } catch (Exception e) {
            throw new PolarisNamingException("select healthy polaris service failed", e);
        }
        return ret;
    }

    /**
     * Subscribe service to receive events of instances alteration.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @param cluster cluster of service
     * @param listener    event listener
     * @throws PolarisNamingException polaris exception
     */
    public void subscribe(String namespace, String serviceName, String cluster, PolarisListener listener) throws PolarisNamingException {

    }

    /**
     * Un-subscribe service to receive events of instances alteration.
     *
     * @param namespace   namespace of service
     * @param serviceName name of service
     * @param cluster cluster of service
     * @param listener    event listener
     * @throws PolarisNamingException polaris exception
     */
    public void unsubscribe(String namespace, String serviceName, String cluster, PolarisListener listener) throws PolarisNamingException {

    }

    // ~~ Inner OpenApi Client Operations

    enum PolarisRegistryRequests implements PolarisRegistryRequest<Request, Response> {

        /**
         * Register Service Instance Api.
         */
        REGISTER_INSTANCE("/naming/v1/instances") {
            /**
             * Execute Request
             *
             * @param request request instance
             * @param options request option settings
             * @return request execute response .
             * @throws Exception maybe throw exception
             */
            @Override
            public RegistryCommonResponse execute(Request request, RequestOptions options) throws Exception {
                try{
                    if (request instanceof RegistryRequest) {
                        RegistryRequest registryRequest = (RegistryRequest) request;

                        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequest.post(REGISTER_INSTANCE.uri())
                            .header(ACCESS_TOKEN_HEADER, properties.token())
                            .contentType(CONTENT_TYPE_JSON, CHARSET_UTF8)
                            // trust all certs & hosts
                            .trustAllCerts().trustAllHosts()
                            .connectTimeout(properties.connectTimeout())
                            .readTimeout(properties.readTimeout())
                            .send(JacksonUtils.serialize2Json(registryRequest.getInstances()));

                        int code = simpleHttpRequest.code();

                        if (HTTP_OK == code) {
                            String body = simpleHttpRequest.body(CHARSET_UTF8);
                            return JacksonUtils.json2JavaBean(body, RegistryCommonResponse.class);
                        }
                        else {
                            LOGGER.warn("[Polaris-Registry] invalid service register response http-code : {}", code);
                        }
                    } else {
                        LOGGER.warn("[Polaris-Registry] invalid service register request class type.");
                    }
                } catch (Exception e) {
                    LOGGER.warn("[Polaris-Registry] service register request execute failed , err-msg : {}", e.getMessage());
                }

                // DEFAULT NULL
                return null;
            }
        },

        /**
         * De-Register Service Instance Api.
         */
        DEREGISTER_INSTANCE("/naming/v1/instances/delete") {

            /**
             * Execute Request
             *
             * @param request request instance
             * @param options request option settings
             * @return request execute response .
             * @throws Exception maybe throw exception
             */
            @Override
            public RegistryCommonResponse execute(Request request, RequestOptions options) throws Exception {
                try{
                    if (request instanceof DeregistryRequest) {
                        DeregistryRequest deregistryRequest = (DeregistryRequest) request;

                        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequest.post(DEREGISTER_INSTANCE.uri())
                            .header(ACCESS_TOKEN_HEADER, properties.token())
                            .contentType(CONTENT_TYPE_JSON, CHARSET_UTF8)
                            // trust all certs & hosts
                            .trustAllCerts().trustAllHosts()
                            .connectTimeout(properties.connectTimeout())
                            .readTimeout(properties.readTimeout())
                            .send(JacksonUtils.serialize2Json(deregistryRequest.getServiceIds()));

                        int code = simpleHttpRequest.code();

                        if (HTTP_OK == code) {
                            String body = simpleHttpRequest.body(CHARSET_UTF8);
                            return JacksonUtils.json2JavaBean(body, RegistryCommonResponse.class);
                        }
                        else {
                            LOGGER.warn("[Polaris-Registry] invalid service de-register response http-code : {}", code);
                        }
                    } else {
                        LOGGER.warn("[Polaris-Registry] invalid service de-register request class type.");
                    }
                } catch (Exception e) {
                    LOGGER.warn("[Polaris-Registry] service de-register request execute failed , err-msg : {}", e.getMessage());
                }

                // DEFAULT NULL
                return null;
            }
        },

        /**
         * Service Instance Heartbeat Request.
         */
        HEARTBEAT("/v1/Heartbeat") {

            /**
             * Execute Request
             *
             * @param request request instance
             * @param options request option settings
             * @return request execute response .
             * @throws Exception maybe throw exception
             */
            @Override
            public HeartbeatResponse execute(Request request, RequestOptions options) throws Exception {
                try{
                    if (request instanceof HeartbeatRequest) {
                        HeartbeatRequest heartbeatRequest = (HeartbeatRequest) request;

                        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequest.post(HEARTBEAT.uri())
                            .header(ACCESS_TOKEN_HEADER, properties.token())
                            .contentType(CONTENT_TYPE_JSON, CHARSET_UTF8)
                            // trust all certs & hosts
                            .trustAllCerts().trustAllHosts()
                            .connectTimeout(properties.connectTimeout())
                            .readTimeout(properties.readTimeout())
                            .send(JacksonUtils.serialize2Json(heartbeatRequest));

                        int code = simpleHttpRequest.code();

                        if (HTTP_OK == code) {
                            String body = simpleHttpRequest.body(CHARSET_UTF8);
                            return JacksonUtils.json2JavaBean(body, HeartbeatResponse.class);
                        }
                        else {
                            LOGGER.warn("[Polaris-Registry] invalid service heartbeat response http-code : {}", code);
                        }
                    } else {
                        LOGGER.warn("[Polaris-Registry] invalid service heartbeat request class type.");
                    }
                } catch (Exception e) {
                    LOGGER.warn("[Polaris-Registry] service heartbeat request execute failed , err-msg : {}", e.getMessage());
                }

                // DEFAULT NULL
                return null;
            }
        },

        /**
         * Get All Instance(s) Request.
         */
        GET_ALL_INSTANCES("/v1/Discover") {

            /**
             * Execute Request
             *
             * @param request request instance
             * @param options request option settings
             * @return request execute response .
             * @throws Exception maybe throw exception
             */
            @Override public GetAllServiceInstancesResponse execute(Request request, RequestOptions options) throws Exception {
                try{
                    if (request instanceof GetAllServiceInstancesRequest) {
                        GetAllServiceInstancesRequest getAllServiceInstancesRequest = (GetAllServiceInstancesRequest) request;

                        SimpleHttpRequest simpleHttpRequest = SimpleHttpRequest.post(REGISTER_INSTANCE.uri())
                            .header(ACCESS_TOKEN_HEADER, properties.token())
                            .contentType(CONTENT_TYPE_JSON, CHARSET_UTF8)
                            // trust all certs & hosts
                            .trustAllCerts().trustAllHosts()
                            .connectTimeout(properties.connectTimeout())
                            .readTimeout(properties.readTimeout())
                            .send(JacksonUtils.serialize2Json(getAllServiceInstancesRequest));

                        int code = simpleHttpRequest.code();

                        if (HTTP_OK == code) {
                            String body = simpleHttpRequest.body(CHARSET_UTF8);
                            return JacksonUtils.json2JavaBean(body, GetAllServiceInstancesResponse.class);
                        }
                        else {
                            LOGGER.warn("[Polaris-Registry] invalid get all services response http-code : {}", code);
                        }
                    } else {
                        LOGGER.warn("[Polaris-Registry] invalid get all services request class type.");
                    }
                } catch (Exception e) {
                    LOGGER.warn("[Polaris-Registry] get all services request execute failed , err-msg : {}", e.getMessage());
                }

                // DEFAULT NULL
                return null;
            }
        }
        ;

        /**
         * Request URL.
         */
        private final String uri;

        PolarisRegistryRequests(String uri) {
            this.uri = uri;
        }

        public String uri() {
            return properties.address().concat(uri);
        }
    }

    // ~~ Models

    /**
     * Polaris Service Key.
     */
    static class ServiceKey {
        final String namespace;
        final String serviceName;
        final String cluster;

        public ServiceKey(String namespace, String serviceName, String cluster) {
            this.namespace = namespace;
            this.serviceName = serviceName;
            this.cluster = cluster;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ServiceKey key = (ServiceKey) o;
            return Objects.equals(namespace, key.namespace) && Objects.equals(serviceName, key.serviceName) && Objects.equals(cluster, key.cluster);
        }

        @Override
        public int hashCode() {
            return Objects.hash(namespace, serviceName, cluster);
        }
    }

    // ~~ Request(s) & Response(s)

    /**
     * Service Registry Request.
     * @see InstanceInfo
     */
    static class RegistryRequest extends Request {

        private final List<InstanceInfo> instances = new ArrayList<>();
        public void addInstance(InstanceInfo instance) {
            if (instance != null) {
                this.instances.add(instance);
            }
        }

        public List<InstanceInfo> getInstances() {
            return instances;
        }

        /**
         * Instance Service Model.
         */
        static class InstanceInfo implements Serializable {
            private String service;
            private String namespace;
            private String host;
            private int port;

            @JsonProperty("enable_health_check")
            private boolean enableHealthCheck = true;

            @JsonProperty("health_check")
            private HealthCheck healthCheck = new HealthCheck();
            private Map<String, String> metadata = new HashMap<>();

            public String getService() {
                return service;
            }

            public String getNamespace() {
                return namespace;
            }

            public String getHost() {
                return host;
            }

            public int getPort() {
                return port;
            }

            public boolean isEnableHealthCheck() {
                return enableHealthCheck;
            }

            public HealthCheck getHealthCheck() {
                return healthCheck;
            }

            public Map<String, String> getMetadata() {
                return metadata;
            }

            public void setService(String service) {
                this.service = service;
            }

            public void setNamespace(String namespace) {
                this.namespace = namespace;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public void setEnableHealthCheck(boolean enableHealthCheck) {
                this.enableHealthCheck = enableHealthCheck;
            }

            public void setHealthCheck(HealthCheck healthCheck) {
                this.healthCheck = healthCheck;
            }

            public void setMetadata(Map<String, String> metadata) {
                this.metadata = metadata;
            }

            static class HealthCheck {
                private int type = 1;
                private Heartbeat heartbeat = new Heartbeat();

                public int getType() {
                    return type;
                }

                public void setType(int type) {
                    this.type = type;
                }

                public Heartbeat getHeartbeat() {
                    return heartbeat;
                }

                public void setHeartbeat(Heartbeat heartbeat) {
                    this.heartbeat = heartbeat;
                }
            }

            static class Heartbeat {
                private int ttl = 10;

                public void setTtl(int ttl) {
                    this.ttl = ttl;
                }

                public int getTtl() {
                    return ttl;
                }
            }
        }
    }

    /**
     * Service De-Register Request.
     */
    static class DeregistryRequest extends Request {

        private final List<String> serviceIds = new ArrayList<>();

        public void addInstance(String serviceId) {
            if (StringUtils.isNotBlank(serviceId)) {
                this.serviceIds.add(serviceId);
            }
        }

        public List<String> getServiceIds() {
            return serviceIds;
        }
    }

    /**
     * Service Registry Response.
     */
    static class RegistryCommonResponse extends Response {

        private int size;

        private List<Response> responses;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public List<Response> getResponses() {
            return responses;
        }

        public void setResponses(List<Response> responses) {
            this.responses = responses;
        }
    }

    /**
     * Service Heartbeat Request.
     */
    static class HeartbeatRequest extends Request {

        private String service;
        private String namespace;
        private String host;
        private int port;

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    /**
     * Service Heartbeat Response.
     */
    static class HeartbeatResponse extends Response {

    }

    /**
     * Get All Instances Request.
     */
    static class GetAllServiceInstancesRequest extends Request {

        /**
         * Default Instance Type Value.
         */
        private static final int INSTANCE_TYPE = 1;

        private final int type;

        private final ServiceMetadata service;

        public GetAllServiceInstancesRequest(ServiceMetadata service) {
            this(INSTANCE_TYPE, service);
        }

        public GetAllServiceInstancesRequest(int type, ServiceMetadata service) {
            this.type = type;
            this.service = service;
        }

        public int getType() {
            return type;
        }

        public ServiceMetadata getService() {
            return service;
        }

        static class ServiceMetadata {
            private final String name;

            private final String namespace;

            public ServiceMetadata(String name, String namespace) {
                this.name = name;
                this.namespace = namespace;
            }

            public String getName() {
                return name;
            }

            public String getNamespace() {
                return namespace;
            }
        }
    }

    /**
     * Get All Instances Response.
     */
    static class GetAllServiceInstancesResponse extends Response {

        private ServiceInfo service;
        private List<InstanceInfo> instances;

        public List<InstanceInfo> getInstances() {
            return instances;
        }

        public void setInstances(List<InstanceInfo> instances) {
            this.instances = instances;
        }

        public ServiceInfo getService() {
            return service;
        }

        public void setService(ServiceInfo service) {
            this.service = service;
        }

        // ~~

        /**
         * Service Info Model.
         */
        static class ServiceInfo implements Serializable {

            private String id;

            private String name;

            private String namespace;

            private String revision;

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getNamespace() {
                return namespace;
            }

            public void setNamespace(String namespace) {
                this.namespace = namespace;
            }

            public String getRevision() {
                return revision;
            }

            public void setRevision(String revision) {
                this.revision = revision;
            }
        }

        /**
         * Instance Service Model.
         */
        static class InstanceInfo implements Serializable {

            private String id;
            private String service;
            private String namespace;
            private String host;
            private int port;
            private Map<String, String> metadata;

            private boolean healthy;

            private boolean enableHealthCheck;

            private HealthCheck healthCheck = new HealthCheck();

            private String revision;

            @JsonProperty("logic_set")
            private String cluster;

            public String getService() {
                return service;
            }

            public String getNamespace() {
                return namespace;
            }

            public String getHost() {
                return host;
            }

            public int getPort() {
                return port;
            }

            public Map<String, String> getMetadata() {
                return metadata;
            }

            public void setService(String service) {
                this.service = service;
            }

            public void setNamespace(String namespace) {
                this.namespace = namespace;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public void setPort(int port) {
                this.port = port;
            }

            public void setMetadata(Map<String, String> metadata) {
                this.metadata = metadata;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public boolean isHealthy() {
                return healthy;
            }

            public void setHealthy(boolean healthy) {
                this.healthy = healthy;
            }

            public String getRevision() {
                return revision;
            }

            public void setRevision(String revision) {
                this.revision = revision;
            }

            public String getCluster() {
                return cluster;
            }

            public void setCluster(String cluster) {
                this.cluster = cluster;
            }

            public boolean isEnableHealthCheck() {
                return enableHealthCheck;
            }

            public void setEnableHealthCheck(boolean enableHealthCheck) {
                this.enableHealthCheck = enableHealthCheck;
            }

            public HealthCheck getHealthCheck() {
                return healthCheck;
            }

            public void setHealthCheck(HealthCheck healthCheck) {
                this.healthCheck = healthCheck;
            }

            static class HealthCheck {

                private Heartbeat heartbeat = new Heartbeat();

                public Heartbeat getHeartbeat() {
                    return heartbeat;
                }

                public void setHeartbeat(Heartbeat heartbeat) {
                    this.heartbeat = heartbeat;
                }
            }

            static class Heartbeat {
                private int ttl = 10;

                public void setTtl(int ttl) {
                    this.ttl = ttl;
                }

                public int getTtl() {
                    return ttl;
                }
            }
        }
    }

    /**
     * Base Request Interface Defined .
     */
    static class Request {

        /**
         * Remote server access token header .
         */
        public static final String ACCESS_TOKEN_HEADER = "X-Polaris-Token";

        /**
         * Request Success Code Defined .
         */
        public static final int RESPONSE_OK = 200000;

        /**
         * Remote Config File NO_CHANGE Response Code.
         */
        public static final int RESPONSE_NO_CHANGE = 200001;
    }

    /**
     * Base Request Response Defined.
     */
    static class Response {

        /**
         * Response Code.
         */
        protected int code;

        /**
         * Response Message Info.
         */
        protected String info;

        protected int getCode() {
            return code;
        }

        protected void setCode(int code) {
            this.code = code;
        }

        protected String getInfo() {
            return info;
        }

        protected void setInfo(String info) {
            this.info = info;
        }

        /**
         * Check Request is ok.
         * @return true otherwise return false.
         */
        public boolean isOk() {
            return RESPONSE_OK == code;
        }
    }

    /**
     * Polaris Config Request Executor Interface .
     *
     * @param <REQ> sub-instance of {@link Request}
     * @param <RES> sub-instance of {@link Response}
     */
    private interface PolarisRegistryRequest<REQ extends Request, RES extends Response> {

        /**
         * Request Options.
         */
        class RequestOptions {

            /**
             * Read Timeout.
             */
            private int readTimeout = 0;

            public int readTimeout() {
                return readTimeout;
            }

            public RequestOptions readTimeout(int readTimeout) {
                this.readTimeout = readTimeout;
                return this;
            }
        }

        /**
         * Execute Request
         *
         * @param request request instance
         * @param options request option settings
         * @return request execute response .
         * @throws Exception maybe throw exception
         */
        RES execute(REQ request, RequestOptions options) throws Exception;

        /**
         * Execute Request
         *
         * @param request request instance
         * @return request execute response .
         * @throws Exception maybe throw exception
         */
        default RES execute(REQ request) throws Exception {
            return execute(request, null);
        }
    }


    // ~~ Local Cache Storage



}
