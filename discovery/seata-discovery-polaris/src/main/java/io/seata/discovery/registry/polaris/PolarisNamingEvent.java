package io.seata.discovery.registry.polaris;

import io.seata.discovery.registry.polaris.client.PolarisInstance;
import java.util.List;

/**
 * {@link PolarisNamingEvent} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-23
 */
public class PolarisNamingEvent {

    private String serviceName;

    private String groupName;

    private String clusters;

    private List<PolarisInstance> instances;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getClusters() {
        return clusters;
    }

    public void setClusters(String clusters) {
        this.clusters = clusters;
    }

    public List<PolarisInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<PolarisInstance> instances) {
        this.instances = instances;
    }
}
