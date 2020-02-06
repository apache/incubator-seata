package io.seata.server.coordinator;

import io.seata.core.model.ResourceManagerOutbound;
import io.seata.core.model.TransactionManager;

/**
 * @author zhangchenghui.dev@gmail.com
 * @since 1.1.0
 */
public interface TransactionCoordinatorInbound extends ResourceManagerOutbound, TransactionManager {

}
