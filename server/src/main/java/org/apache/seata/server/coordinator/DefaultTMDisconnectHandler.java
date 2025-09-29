/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.coordinator;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TMDisconnectHandler;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * Default implementation of TMDisconnectHandler.
 * Handles TM disconnection and performs early rollback of global transactions
 * when enabled via configuration.
 */
public class DefaultTMDisconnectHandler implements TMDisconnectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTMDisconnectHandler.class);

    private final Core core;

    /**
     * Constructor with core dependency
     *
     * @param core the transaction core
     */
    public DefaultTMDisconnectHandler(Core core) {
        this.core = core;
    }

    @Override
    public void handleTMDisconnect(RpcContext rpcContext) {
        if (rpcContext == null || rpcContext.getTransactionServiceGroup() == null) {
            return;
        }

        // Check if early rollback is enabled
        boolean enableRollbackWhenDisconnect = ConfigurationFactory.getInstance()
                .getBoolean(
                        ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                        DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT);

        if (!enableRollbackWhenDisconnect) {
            LOGGER.debug("Early rollback on TM disconnect is disabled");
            return;
        }

        String transactionServiceGroup = rpcContext.getTransactionServiceGroup();
        String applicationId = rpcContext.getApplicationId();

        LOGGER.info(
                "TM disconnected: transactionServiceGroup={}, applicationId={}, performing early rollback check",
                transactionServiceGroup,
                applicationId);

        try {
            // Find all global sessions in BEGIN status
            Collection<GlobalSession> globalSessions =
                    SessionHolder.getRootSessionManager().allSessions();

            int rollbackCount = 0;
            for (GlobalSession globalSession : globalSessions) {
                if (shouldRollbackSession(globalSession, rpcContext)) {
                    try {
                        LOGGER.info(
                                "Early rollback for TM disconnect: xid={}, transactionServiceGroup={}, applicationId={}",
                                globalSession.getXid(),
                                globalSession.getTransactionServiceGroup(),
                                globalSession.getApplicationId());

                        // Change status to TimeoutRollbacking
                        globalSession.changeGlobalStatus(GlobalStatus.TimeoutRollbacking);

                        // Perform rollback
                        core.doGlobalRollback(globalSession, false);
                        rollbackCount++;

                    } catch (TransactionException e) {
                        LOGGER.error(
                                "Failed to rollback transaction [{}] {} {}",
                                globalSession.getXid(),
                                e.getCode(),
                                e.getMessage());
                    }
                }
            }

            if (rollbackCount > 0) {
                LOGGER.info(
                        "Early rollback completed for TM disconnect: transactionServiceGroup={}, rollbackCount={}",
                        transactionServiceGroup,
                        rollbackCount);
            }

        } catch (Exception e) {
            LOGGER.error(
                    "Error during TM disconnect handling for transactionServiceGroup={}", transactionServiceGroup, e);
        }
    }

    /**
     * Get the session manager instance. Made public for testing purposes.
     *
     * @return the session manager
     */
    public SessionManager getSessionManager() {
        return SessionHolder.getRootSessionManager();
    }

    /**
     * Determine if a global session should be rolled back based on the disconnected TM context.
     * Uses community-approved matching algorithm:
     * 1. Primary: TransactionServiceGroup matching (vgroup approach)
     * 2. Secondary: ApplicationId matching for additional safety
     * 3. Only rollback sessions in BEGIN status
     *
     * @param globalSession the global session to check
     * @param tmContext the disconnected TM context
     * @return true if the session should be rolled back
     */
    private boolean shouldRollbackSession(GlobalSession globalSession, RpcContext tmContext) {
        // Only rollback sessions in BEGIN status
        if (globalSession.getStatus() != GlobalStatus.Begin) {
            return false;
        }

        // Primary identifier: TransactionServiceGroup (vgroup) matching
        // This is the community consensus approach from Issue #4422
        if (!Objects.equals(globalSession.getTransactionServiceGroup(), tmContext.getTransactionServiceGroup())) {
            return false;
        }

        // Secondary safety check: ApplicationId matching when both are available
        // This provides additional confidence to prevent false positives
        if (globalSession.getApplicationId() != null && tmContext.getApplicationId() != null) {
            return Objects.equals(globalSession.getApplicationId(), tmContext.getApplicationId());
        }

        // If applicationId is not available on both sides, trust the vgroup match
        // This follows the community approach that vgroup is the primary identifier
        return true;
    }
}
