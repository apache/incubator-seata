package io.seata.server.store;

import io.seata.common.XID;
import io.seata.common.metadata.Instance;
import io.seata.core.store.MappingDO;
import io.seata.discovery.registry.MultiRegistryFactory;
import io.seata.discovery.registry.RegistryService;

import java.net.InetSocketAddress;
import java.util.HashMap;

public interface VGroupMappingStoreManager {
    /**
     * add VGroup Mapping relationship in cluster
     *
     * @param mappingDO the relationship between vGroup and Cluster
     */
    boolean addVGroup(MappingDO mappingDO);

    /**
     * remove VGroup Mapping relationship in cluster
     *
     * @param vGroup
     */
    boolean removeVGroup(String vGroup);

    /**
     * get VGroup Mapping relationship in cluster
     *
     * @return
     */
    HashMap<String, Object> load();

    /**
     * notify mapping relationship to all namingserver nodes
     */
    default void notifyMapping() {

        Instance instance = Instance.getInstance();
        instance.addMetadata("vGroup", this.load());
        try {
            InetSocketAddress address = new InetSocketAddress(XID.getIpAddress(), XID.getPort());
            for (RegistryService registryService : MultiRegistryFactory.getInstances()) {
                registryService.register(address);
            }
        } catch (Exception e) {
            throw new RuntimeException("vGroup mapping relationship notified failed! ", e);
        }
    }

}
