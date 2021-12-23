package io.seata.rm.datasource.xa;

import io.seata.common.DefaultValues;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.rpc.hook.NettyClientTimeoutCheckerHook;
import io.seata.rm.BaseDataSourceResource;
import io.seata.rm.DefaultResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author longchenming
 */
public class RmNettyClientTimeoutCheckerHookXA implements NettyClientTimeoutCheckerHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(RmNettyClientTimeoutCheckerHookXA.class);

    @Override
    public void doBeforeChecker(String transactionServiceGroup) {
        //do nothing
    }

    @Override
    public void doAfterChecker(String transactionServiceGroup) {
        Map<String, Resource> resourceMap = DefaultResourceManager.get().getResourceManager(BranchType.XA).getManagedResources();
        for (Map.Entry<String, Resource> entry : resourceMap.entrySet()) {
            BaseDataSourceResource resource = (BaseDataSourceResource) entry.getValue();
            Map<String, ConnectionProxyXA> keeper = resource.getKeeper();
            for (Map.Entry<String, ConnectionProxyXA> connectionEntry : keeper.entrySet()) {
                ConnectionProxyXA connection = connectionEntry.getValue();
                long now = System.currentTimeMillis();
                synchronized (connection) {
                    if (connection.getPrepareTime() != null &&
                            now - connection.getPrepareTime() > DefaultValues.DEFAULT_XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT) {
                        try {
                            connection.closeForce();
                        } catch (SQLException e) {
                            LOGGER.info("Force close the xa physical connection fail", e);
                        }
                    }
                }
            }
        }
    }

}
