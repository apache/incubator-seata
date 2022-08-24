package io.seata.discovery.registry.polaris;

import java.util.List;

import io.seata.discovery.registry.polaris.client.PolarisInstance;

import static io.seata.discovery.registry.polaris.PolarisRegistryServiceImpl.DEFAULT_CLUSTER;

/**
 * {@link PolarisNamingEvent} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-23
 */
public class PolarisNamingEvent {

    private String serviceName;

    private String namespace;

    private String cluster = DEFAULT_CLUSTER;

    private List<PolarisInstance> instances;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public List<PolarisInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<PolarisInstance> instances) {
        this.instances = instances;
    }
}
