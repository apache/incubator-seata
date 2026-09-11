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

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import io.seata.common.DefaultValues;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.rm.BaseDataSourceResource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.util.SeataXAResource;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.ConfigurationKeys.XA_BRANCH_EXECUTION_TIMEOUT;

/**
 * Connection proxy for XA mode.
 *
 * @author sharajava
 */
public class ConnectionProxyXA extends AbstractConnectionProxyXA implements Holdable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProxyXA.class);

    private static final int BRANCH_EXECUTION_TIMEOUT = ConfigurationFactory.getInstance().getInt(XA_BRANCH_EXECUTION_TIMEOUT,
            DefaultValues.DEFAULT_XA_BRANCH_EXECUTION_TIMEOUT);

    private volatile boolean currentAutoCommitStatus = true;

    private volatile XAXid xaBranchXid;

    private volatile boolean xaActive = false;

    private volatile boolean kept = false;

    private volatile boolean rollBacked = false;

    private volatile Long branchRegisterTime = null;

    private volatile Long prepareTime = null;

    private static final Integer TIMEOUT = Math.max(BRANCH_EXECUTION_TIMEOUT, DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT);

    private boolean shouldBeHeld = false;

    /**
     * Constructor of Connection Proxy for XA mode.
     *
     * @param originalConnection Normal Connection from the original DataSource.
     * @param xaConnection XA Connection based on physical connection of the normal Connection above.
     * @param resource The corresponding Resource(DataSource proxy) from which the connections was created.
     * @param xid Seata global transaction xid.
     */
    public ConnectionProxyXA(Connection originalConnection, XAConnection xaConnection, BaseDataSourceResource resource,
        String xid) {
        super(originalConnection, xaConnection, resource, xid);
        this.shouldBeHeld = resource.isShouldBeHeld();
    }

    public void init() {
        try {
            this.xaResource = xaConnection.getXAResource();
            this.currentAutoCommitStatus = this.originalConnection.getAutoCommit();
            if (!currentAutoCommitStatus) {
                throw new IllegalStateException("Connection[autocommit=false] as default is NOT supported");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void keepIfNecessary() {
        if (shouldBeHeld()) {
            resource.hold(xaBranchXid.toString(), this);
        }
    }

    private void releaseIfNecessary() {
        if (shouldBeHeld()) {
            if (this.xaBranchXid != null) {
                String xaBranchXid = this.xaBranchXid.toString();
                if (isHeld()) {
                    resource.release(xaBranchXid, this);
                }
            }
        }
    }

    /**
     * XA commit
     * @param xid global transaction xid
     * @param branchId transaction branch id
     * @param applicationData application data
     * @throws SQLException SQLException
     */
    public synchronized void xaCommit(String xid, long branchId, String applicationData) throws XAException {
        XAXid xaXid = XAXidBuilder.build(xid, branchId);
        xaResource.commit(xaXid, false);
        releaseIfNecessary();
    }

    /**
     * XA rollback
     * @param xid global transaction xid
     * @param branchId transaction branch id
     * @param applicationData application data
     */
    public synchronized void xaRollback(String xid, long branchId, String applicationData) throws XAException {
        XAXid xaXid = XAXidBuilder.build(xid, branchId);
        xaRollback(xaXid);
    }

    /**
     * XA rollback
     * @param xaXid xaXid
     * @throws XAException XAException
     */
    public void xaRollback(XAXid xaXid) throws XAException {
        xaResource.rollback(xaXid);
        releaseIfNecessary();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (currentAutoCommitStatus == autoCommit) {
            return;
        }
        if (autoCommit) {
            // According to JDBC spec:
            // If this method is called during a transaction and the
            // auto-commit mode is changed, the transaction is committed.
            if (xaActive) {
                commit();
            }
        } else {
            if (xaActive) {
                throw new SQLException("should NEVER happen: setAutoCommit from true to false while xa branch is active");
            }
            // Start a XA branch
            long branchId;
            try {
                // 1. register branch to TC then get the branch message
                branchRegisterTime = System.currentTimeMillis();
                branchId = DefaultResourceManager.get().branchRegister(BranchType.XA, resource.getResourceId(), null, xid, null,
                        null);
            } catch (TransactionException te) {
                cleanXABranchContext();
                throw new SQLException("failed to register xa branch " + xid + " since " + te.getCode() + ":" + te.getMessage(), te);
            }
            // 2. build XA-Xid with xid and branchId
            this.xaBranchXid = XAXidBuilder.build(xid, branchId);
            // Keep the Connection if necessary
            keepIfNecessary();
            try {
                start();
            } catch (XAException e) {
                cleanXABranchContext();
                throw new SQLException("failed to start xa branch " + xid + " since " + e.getMessage(), e);
            }
            // 4. XA is active
            this.xaActive = true;

        }

        currentAutoCommitStatus = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return currentAutoCommitStatus;
    }

    @Override
    public synchronized void commit() throws SQLException {
        if (currentAutoCommitStatus) {
            // Ignore the committing on an autocommit session.
            return;
        }
        if (!xaActive || this.xaBranchXid == null) {
            throw new SQLException("should NOT commit on an inactive session", SQLSTATE_XA_NOT_END);
        }
        try {
            // XA End: Success
            end(XAResource.TMSUCCESS);
            long now = System.currentTimeMillis();
            checkTimeout(now);
            setPrepareTime(now);
            xaResource.prepare(xaBranchXid);
        } catch (XAException xe) {
            // Branch Report to TC: Failed
            reportStatusToTC(BranchStatus.PhaseOne_Failed);
            throw new SQLException(
                "Failed to end(TMSUCCESS)/prepare xa branch on " + xid + "-" + xaBranchXid.getBranchId() + " since " + xe
                    .getMessage(), xe);
        } finally {
            cleanXABranchContext();
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (currentAutoCommitStatus) {
            // Ignore the committing on an autocommit session.
            return;
        }
        if (!xaActive || this.xaBranchXid == null) {
            throw new SQLException("should NOT rollback on an inactive session");
        }
        try {
            if (!rollBacked) {
                // XA End: Fail
                xaResource.end(this.xaBranchXid, XAResource.TMFAIL);
                xaRollback(xaBranchXid);
            }
            // Branch Report to TC
            reportStatusToTC(BranchStatus.PhaseOne_Failed);
            LOGGER.info("{} was rollbacked", xaBranchXid);
        } catch (XAException xe) {
            throw new SQLException("Failed to end(TMFAIL) xa branch on " + xid + "-" + xaBranchXid.getBranchId()
                + " since " + xe.getMessage(), xe);
        } finally {
            cleanXABranchContext();
        }
    }

    private synchronized void start() throws XAException, SQLException {
        // 3. XA Start
        if (JdbcConstants.ORACLE.equals(resource.getDbType())) {
            xaResource.start(this.xaBranchXid, SeataXAResource.ORATRANSLOOSE);
        } else {
            xaResource.start(this.xaBranchXid, XAResource.TMNOFLAGS);
        }

        try {
            termination();
        } catch (SQLException e) {
            // the framework layer does not actively call ROLLBACK when setAutoCommit throws an SQL exception
            xaResource.end(this.xaBranchXid, XAResource.TMFAIL);
            xaRollback(xaBranchXid);
            // Branch Report to TC: Failed
            reportStatusToTC(BranchStatus.PhaseOne_Failed);
            throw  e;
        }
    }

    private synchronized void end(int flags) throws XAException, SQLException {
        xaResource.end(xaBranchXid, flags);
        termination();
    }

    private void cleanXABranchContext() {
        branchRegisterTime = null;
        prepareTime = null;
        xaActive = false;
        if (!isHeld()) {
            xaBranchXid = null;
        }
    }

    private void checkTimeout(Long now) throws XAException {
        if (now - branchRegisterTime > TIMEOUT) {
            xaRollback(xaBranchXid);
            throw new XAException("XA branch timeout error");
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        rollBacked = false;
        if (isHeld() && shouldBeHeld()) {
            // if kept by a keeper, just hold the connection.
            return;
        }
        cleanXABranchContext();
        originalConnection.close();
    }

    protected synchronized void closeForce() throws SQLException {
        Connection physicalConn = getWrappedConnection();
        if (physicalConn instanceof PooledConnection) {
            physicalConn = ((PooledConnection) physicalConn).getConnection();
        }
        // Force close the physical connection
        physicalConn.close();
        rollBacked = false;
        cleanXABranchContext();
        originalConnection.close();
        releaseIfNecessary();
    }

    @Override
    public void setHeld(boolean kept) {
        this.kept = kept;
    }

    @Override
    public boolean isHeld() {
        return kept;
    }

    @Override
    public boolean shouldBeHeld() {
        return shouldBeHeld || StringUtils.isBlank(resource.getDbType());
    }

    public Long getPrepareTime() {
        return prepareTime;
    }

    private void setPrepareTime(Long prepareTime) {
        this.prepareTime = prepareTime;
    }

    private void termination() throws SQLException {
        termination(this.xaBranchXid.toString());
    }

    private void termination(String xaBranchXid) throws SQLException {
        // if it is not empty, the resource will hang and need to be terminated early
        BranchStatus branchStatus = BaseDataSourceResource.getBranchStatus(xaBranchXid);
        if (branchStatus != null) {
            releaseIfNecessary();
            throw new SQLException("failed xa branch " + xid
                    + " the global transaction has finish, branch status: " + branchStatus.getCode());
        }
    }

    /**
     * Report branch status to TC
     *
     * @param status branch status
     */
    private void reportStatusToTC(BranchStatus status) {
        try {
            DefaultResourceManager.get().branchReport(BranchType.XA, xid, xaBranchXid.getBranchId(),
                    status, null);
        } catch (TransactionException te) {
            LOGGER.warn("Failed to report XA branch {} on {}-{} since {}:{}",
                    status, xid, xaBranchXid.getBranchId(), te.getCode(), te.getMessage());
        }
    }

}
