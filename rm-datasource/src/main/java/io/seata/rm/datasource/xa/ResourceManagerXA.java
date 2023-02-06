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

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.sql.SQLException;
import javax.transaction.xa.XAException;
import io.seata.common.DefaultValues;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.config.ConfigurationFactory;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.BaseDataSourceResource;
import io.seata.rm.datasource.AbstractDataSourceCacheResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.constants.ConfigurationKeys.XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT;

/**
 * RM for XA mode.
 *
 * @author sharajava
 */
public class ResourceManagerXA extends AbstractDataSourceCacheResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerXA.class);

    private static final int TWO_PHASE_HOLD_TIMEOUT = ConfigurationFactory.getInstance().getInt(XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT,
            DefaultValues.DEFAULT_XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT);

    private static final long SCHEDULE_DELAY_MILLS = 60 * 1000L;
    private static final long SCHEDULE_INTERVAL_MILLS = 1000L;
    /**
     * The Timer check xa branch two phase hold timeout.
     */
    protected volatile ScheduledExecutorService xaTwoPhaseTimeoutChecker;

    @Override
    public void init() {
        LOGGER.info("ResourceManagerXA init ...");
    }

    public void initXaTwoPhaseTimeoutChecker() {
        if (xaTwoPhaseTimeoutChecker == null) {
            synchronized (this) {
                if (xaTwoPhaseTimeoutChecker == null) {
                    boolean shouldBeHold = dataSourceCache.values().parallelStream().anyMatch(resource -> {
                        if (resource instanceof DataSourceProxyXA) {
                            return ((DataSourceProxyXA)resource).isShouldBeHeld();
                        }
                        return false;
                    });
                    if (shouldBeHold) {
                        xaTwoPhaseTimeoutChecker = new ScheduledThreadPoolExecutor(1,
                            new NamedThreadFactory("xaTwoPhaseTimeoutChecker", 1, true));
                        xaTwoPhaseTimeoutChecker.scheduleAtFixedRate(() -> {
                            for (Map.Entry<String, Resource> entry : dataSourceCache.entrySet()) {
                                BaseDataSourceResource resource = (BaseDataSourceResource)entry.getValue();
                                if (resource.isShouldBeHeld()) {
                                    if (resource instanceof DataSourceProxyXA) {
                                        Map<String, ConnectionProxyXA> keeper = resource.getKeeper();
                                        for (Map.Entry<String, ConnectionProxyXA> connectionEntry : keeper.entrySet()) {
                                            ConnectionProxyXA connection = connectionEntry.getValue();
                                            long now = System.currentTimeMillis();
                                            synchronized (connection) {
                                                if (connection.getPrepareTime() != null
                                                    && now - connection.getPrepareTime() > TWO_PHASE_HOLD_TIMEOUT) {
                                                    try {
                                                        connection.closeForce();
                                                    } catch (SQLException e) {
                                                        LOGGER.warn("Force close the xa physical connection fail", e);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }, SCHEDULE_DELAY_MILLS, SCHEDULE_INTERVAL_MILLS, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.XA;
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {
        return finishBranch(true, branchType, xid, branchId, resourceId, applicationData);
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        return finishBranch(false, branchType, xid, branchId, resourceId, applicationData);
    }

    private BranchStatus finishBranch(boolean committed, BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        XAXid xaBranchXid = XAXidBuilder.build(xid, branchId);
        Resource resource = dataSourceCache.get(resourceId);
        if (resource instanceof AbstractDataSourceProxyXA) {
            try (ConnectionProxyXA connectionProxyXA =
                ((AbstractDataSourceProxyXA)resource).getConnectionForXAFinish(xaBranchXid)) {
                if (committed) {
                    connectionProxyXA.xaCommit(xid, branchId, applicationData);
                    LOGGER.info(xaBranchXid + " was committed.");
                    return BranchStatus.PhaseTwo_Committed;
                } else {
                    connectionProxyXA.xaRollback(xid, branchId, applicationData);
                    LOGGER.info(xaBranchXid + " was rollbacked");
                    return BranchStatus.PhaseTwo_Rollbacked;
                }
            } catch (XAException | SQLException sqle) {
                if (sqle instanceof XAException) {
                    try {
                        if (((XAException) sqle).errorCode == XAException.XAER_NOTA) {
                            if (committed) {
                                return BranchStatus.PhaseTwo_CommitFailed_XAER_NOTA_Retryable;
                            } else {
                                return BranchStatus.PhaseTwo_RollbackFailed_XAER_NOTA_Retryable;
                            }
                        }
                    } finally {
                        BaseDataSourceResource.setBranchStatus(xaBranchXid.toString(),
                                committed ? BranchStatus.PhaseTwo_Committed : BranchStatus.PhaseTwo_Rollbacked);
                    }
                }
                if (committed) {
                    LOGGER.error(xaBranchXid + " commit failed since " + sqle.getMessage(), sqle);
                    // FIXME: case of PhaseTwo_CommitFailed_Unretryable
                    return BranchStatus.PhaseTwo_CommitFailed_Retryable;
                } else {
                    LOGGER.error(xaBranchXid + " rollback failed since " + sqle.getMessage(), sqle);
                    // FIXME: case of PhaseTwo_RollbackFailed_Unretryable
                    return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
                }
            }
        } else {
            LOGGER.error("Unknown Resource for XA resource " + resourceId + " " + resource);
            if (committed) {
                return BranchStatus.PhaseTwo_CommitFailed_Unretryable;
            } else {
                return BranchStatus.PhaseTwo_RollbackFailed_Unretryable;
            }
        }
    }
}
