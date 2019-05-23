package io.seata.core.rpc.netty.pool;

import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.rpc.netty.NettyPoolKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

import static io.seata.common.Constants.DBKEYS_SPLIT_CHAR;

/**
 * RM client pool key generator.
 *
 * @author zhaojun
 */
public final class RmClientPoolKeyGenerator implements ClientPoolKeyGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RmClientPoolKeyGenerator.class);
    
    private final String applicationId;
    
    private final String transactionServiceGroup;
    
    private final ResourceManager resourceManager;
    
    private final String customerKeys;
    
    public RmClientPoolKeyGenerator(final String applicationId, final String transactionServiceGroup,
                                    final ResourceManager resourceManager, final String customerKeys) {
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
        this.resourceManager = resourceManager;
        this.customerKeys = customerKeys;
    }
    
    @Override
    public NettyPoolKey generate(final String serverAddress) {
        String resourceIds = customerKeys == null ? getMergedResourceKeys() : customerKeys;
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("RM will register :" + resourceIds);
        }
        RegisterRMRequest message = new RegisterRMRequest(applicationId, transactionServiceGroup);
        message.setResourceIds(resourceIds);
        return new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, serverAddress, message);
    }
    
    /**
     * Gets merged resource keys.
     *
     * @return the merged resource keys
     */
    public String getMergedResourceKeys() {
        Map<String, Resource> managedResources = resourceManager.getManagedResources();
        Set<String> resourceIds = managedResources.keySet();
        if (!resourceIds.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String resourceId : resourceIds) {
                if (first) {
                    first = false;
                } else {
                    sb.append(DBKEYS_SPLIT_CHAR);
                }
                sb.append(resourceId);
            }
            return sb.toString();
        }
        return null;
    }
}
