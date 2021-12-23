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
