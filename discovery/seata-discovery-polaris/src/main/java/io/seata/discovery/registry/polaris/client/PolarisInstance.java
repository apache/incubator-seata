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

import java.util.Map;

/**
 * {@link PolarisInstance} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-23
 */
public class PolarisInstance {

    private String namespace;

    private String serviceName;

    private String serviceId;

    private String host;

    private int port;

    private boolean healthy;

    private boolean enableHealthCheck;

    private int healthCheckPeriod;

    private Map<String, String> metadata;

    private String cluster;

    private String revision;

    public PolarisInstance() {
    }

    public PolarisInstance(String namespace, String serviceName, String serviceId, String host, int port,
        boolean healthy,
        boolean enableHealthCheck, int healthCheckPeriod, Map<String, String> metadata, String cluster,
        String revision) {
        this.namespace = namespace;
        this.serviceName = serviceName;
        this.serviceId = serviceId;
        this.host = host;
        this.port = port;
        this.healthy = healthy;
        this.enableHealthCheck = enableHealthCheck;
        this.healthCheckPeriod = healthCheckPeriod;
        this.metadata = metadata;
        this.cluster = cluster;
        this.revision = revision;
    }

    public String getNamespace() {
        return namespace;
    }

    public PolarisInstance setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public PolarisInstance setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public String getServiceId() {
        return serviceId;
    }

    public PolarisInstance setServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public String getHost() {
        return host;
    }

    public PolarisInstance setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public PolarisInstance setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isEnableHealthCheck() {
        return enableHealthCheck;
    }

    public PolarisInstance setEnableHealthCheck(boolean enableHealthCheck) {
        this.enableHealthCheck = enableHealthCheck;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public PolarisInstance setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public String getCluster() {
        return cluster;
    }

    public PolarisInstance setCluster(String cluster) {
        this.cluster = cluster;
        return this;
    }

    public String getRevision() {
        return revision;
    }

    public PolarisInstance setRevision(String revision) {
        this.revision = revision;
        return this;
    }

    public int getHealthCheckPeriod() {
        return healthCheckPeriod;
    }

    public PolarisInstance setHealthCheckPeriod(int healthCheckPeriod) {
        this.healthCheckPeriod = healthCheckPeriod;
        return this;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public PolarisInstance setHealthy(boolean healthy) {
        this.healthy = healthy;
        return this;
    }
}
