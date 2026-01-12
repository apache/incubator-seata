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
package org.apache.seata.rm.datasource.xa;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.lock.ResourceLock;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.DBType;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.BaseDataSourceResource;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.datasource.util.SeataXAResource;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.postgresql.xa.PGXAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.seata.common.ConfigurationKeys.*;

/**
 * Connection proxy for XA mode.
 *
 */
public class ConnectionProxyXA extends AbstractConnectionProxyXA implements Holdable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProxyXA.class);

    private static final int BRANCH_EXECUTION_TIMEOUT = ConfigurationFactory.getInstance()
            .getInt(XA_BRANCH_EXECUTION_TIMEOUT, DefaultValues.DEFAULT_XA_BRANCH_EXECUTION_TIMEOUT);

    private static final String DUMMY_TABLE =
            ConfigurationFactory.getInstance().getConfig(SONATA_DUMMY_TABLE, DefaultValues.DEFAULT_SONATA_DUMMY_TABLE);

    private static final int DUMMY_TABLE_SIZE = ConfigurationFactory.getInstance()
            .getInt(SONATA_DUMMY_TABLE_SIZE, DefaultValues.DEFAULT_SONATA_DUMMY_TABLE_SIZE);

    private static final int S2PL_UPDATE_RETRY_WARNING_THRESHOLD = ConfigurationFactory.getInstance()
            .getInt(
                    SONATA_S2PL_DUMMY_WRITE_RETRY_WARNING_THRESHOLD,
                    DefaultValues.DEFAULT_SONATA_S2PL_DUMMY_WRITE_RETRY_WARNING_THRESHOLD);

    private static final int SSI_HELPER_BATCH_SIZE = ConfigurationFactory.getInstance()
            .getInt(SONATA_SSI_HELPER_BATCH_SIZE, DefaultValues.DEFAULT_SONATA_SSI_HELPER_BATCH_SIZE);

    private volatile boolean currentAutoCommitStatus = true;

    private volatile XAXid xaBranchXid;

    private volatile boolean xaActive = false;

    private volatile boolean kept = false;

    private volatile boolean rollBacked = false;

    private volatile Long branchRegisterTime = null;

    private volatile Long prepareTime = null;

    private static final Integer TIMEOUT =
            Math.max(BRANCH_EXECUTION_TIMEOUT, DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT);

    private boolean shouldBeHeld = false;

    private final ResourceLock resourceLock = new ResourceLock();

    private volatile boolean combine = false;

    /**
     * Constructor of Connection Proxy for XA mode.
     *
     * @param originalConnection Normal Connection from the original DataSource.
     * @param xaConnection XA Connection based on physical connection of the normal Connection above.
     * @param resource The corresponding Resource(DataSource proxy) from which the connections was created.
     * @param xid Seata global transaction xid.
     */
    public ConnectionProxyXA(
            Connection originalConnection, XAConnection xaConnection, BaseDataSourceResource resource, String xid) {
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

    private void xaEnd(XAXid xaXid, int flags) throws XAException {
        if (xaActive) {
            xaResource.end(xaXid, flags);
            xaActive = false;
        }
    }

    /**
     * XA commit
     * @param xid global transaction xid
     * @param branchId transaction branch id
     * @param applicationData application data
     * @throws SQLException SQLException
     */
    public void xaCommit(String xid, long branchId, String applicationData) throws XAException {
        try (ResourceLock ignored = resourceLock.obtain()) {
            XAXid xaXid = XAXidBuilder.build(xid, branchId);

            if (((DataSourceProxyXA) resource).sonataSsiShimEnabled) {
                releaseHelperTxn(xaXid);
            }

            xaResource.commit(xaXid, false);

            if (((DataSourceProxyXA) resource).sonataShimEnabled) {
                forgetDummyKey(xaXid);
            }

            releaseIfNecessary();
        }
    }

    /**
     * XA rollback
     * @param xid global transaction xid
     * @param branchId transaction branch id
     * @param applicationData application data
     */
    public void xaRollback(String xid, long branchId, String applicationData) throws XAException {
        try (ResourceLock ignored = resourceLock.obtain()) {
            if (this.xaBranchXid != null) {
                xaRollback(xaBranchXid);
            } else {
                XAXid xaXid = XAXidBuilder.build(xid, branchId);
                xaRollback(xaXid);
            }
        }
    }

    /**
     * XA rollback
     * @param xaXid xaXid
     * @throws XAException XAException
     */
    public void xaRollback(XAXid xaXid) throws XAException {
        boolean shouldReleaseHelper = ((DataSourceProxyXA) resource).sonataSsiShimEnabled;
        boolean helperReleased = false;

        if (shouldReleaseHelper) {
            helperReleased = releaseHelperTxn(xaXid);
        }

        try {
            xaEnd(xaXid, XAResource.TMFAIL);
        } catch (XAException e) {
            boolean shouldIgnore = false;

            // In MySQL, a branch can be in NON-EXISTING state, but still appears in `xa recover`'s output, so that
            // `xa end` would fail but `xa rollback` would still succeed. We thus suppress this xa end error.
            // Also, deadlock would automatically "roll back" branch txn, triggering error 1614 (already rolled back).
            // Still, we are able to call "XA ROLLBACK xid". So we mask this case as well.
            if (DBType.MYSQL.name().equalsIgnoreCase(resource.getDbType())) {
                Throwable cause = e.getCause();
                if (cause instanceof SQLException) {
                    int error = ((SQLException) cause).getErrorCode();
                    if (error == 1614 || (error == 1399 && cause.getMessage().contains("NON-EXISTING"))) {
                        shouldIgnore = true;
                    }
                }
            }

            // In PG, when a connection is not in the ACTIVE state or its internal xid does not equal the given xid, it
            // raises errors. This could happen when an RM restarts (all connections are fresh) and TM asks it to roll
            // back branches.
            if (e instanceof PGXAException
                    && e.getMessage().contains("tried to call end without corresponding start call")) {
                shouldIgnore = true;
            }

            if (!shouldIgnore) {
                throw e;
            }
        }
        xaResource.rollback(xaXid);

        if (((DataSourceProxyXA) resource).sonataShimEnabled) {
            if (shouldReleaseHelper && !helperReleased) {
                // If we check helper txn ID earlier than RM saving the ID, and rolling back after RM preparing the
                // branch, then the helper txn would be dangling. Since at this point we've successfully rolled back the
                // branch, the helper txn must have ID saved (if there is any) and prepared, otherwise we would fail to
                // roll back. Thus, we try again.
                // Note that, the 2nd try does not always roll back a helper txn. E.g., branch rollback caused by user
                // exceptions, branch prepare is not called at all and there is no helper txn to roll back. Thus, we
                // don't check the return boolean.
                releaseHelperTxn(xaXid);
            }
            forgetDummyKey(xaXid);
        }

        releaseIfNecessary();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (currentAutoCommitStatus == autoCommit) {
            return;
        }
        if (isReadOnly()) {
            // If it is a read-only transaction, do nothing
            currentAutoCommitStatus = autoCommit;
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
            if (this.xaBranchXid != null && currentAutoCommitStatus) {
                return;
            }
            if (xaActive) {
                throw new SQLException(
                        "should NEVER happen: setAutoCommit from true to false while xa branch is active");
            }
            // Start a XA branch
            long branchId;
            try {
                // 1. register branch to TC then get the branch message
                branchRegisterTime = System.currentTimeMillis();
                branchId = DefaultResourceManager.get()
                        .branchRegister(BranchType.XA, resource.getResourceId(), null, xid, null, null);
            } catch (TransactionException te) {
                cleanXABranchContext();
                throw new SQLException(
                        "failed to register xa branch " + xid + " since " + te.getCode() + ":" + te.getMessage(), te);
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
    public void commit() throws SQLException {
        try (ResourceLock ignored = resourceLock.obtain()) {
            if (combine) {
                return;
            }
            if (currentAutoCommitStatus || isReadOnly()) {
                // Ignore the committing on an autocommit session and read-only transaction.
                return;
            }
            if (!xaActive || this.xaBranchXid == null) {
                throw new SQLException("should NOT commit on an inactive session", SQLSTATE_XA_NOT_END);
            }
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (combine) {
            return;
        }
        if (currentAutoCommitStatus || isReadOnly()) {
            // Ignore the committing on an autocommit session and read-only transaction.
            return;
        }
        if (!xaActive || this.xaBranchXid == null) {
            throw new SQLException("should NOT rollback on an inactive session");
        }
        try {
            if (!rollBacked) {
                // XA End: Fail
                try {
                    xaEnd(xaBranchXid, XAResource.TMFAIL);
                } catch (XAException e) {
                    // In MySQL, deadlock would automatically "roll back" branch txn, triggering error 1614. Still, we
                    // are able to call "XA ROLLBACK xid". So we just mask this exception for this particular case.
                    if (!DBType.MYSQL.name().equalsIgnoreCase(resource.getDbType())
                            || !(e.getCause() instanceof SQLException)
                            || ((SQLException) e.getCause()).getErrorCode() != 1614) {
                        throw e;
                    }
                }
                xaRollback(xaBranchXid);
            }
            // Branch Report to TC
            reportStatusToTC(BranchStatus.PhaseOne_Failed);
            LOGGER.info("{} was rollbacked", xaBranchXid);
        } catch (XAException xe) {
            throw new SQLException(
                    "Failed to end(TMFAIL) xa branch on " + xid + "-" + xaBranchXid.getBranchId() + " since "
                            + xe.getMessage(),
                    xe);
        } finally {
            cleanXABranchContext();
        }
    }

    private void start() throws XAException, SQLException {
        try (ResourceLock ignored = resourceLock.obtain()) {
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
                throw e;
            }
        }
    }

    private synchronized void end(int flags) throws XAException, SQLException {
        xaEnd(xaBranchXid, flags);
        termination();
    }

    private void cleanXABranchContext() {
        branchRegisterTime = null;
        prepareTime = null;
        xaActive = false;
        if (!isHeld()) {
            xaBranchXid = null;
        }
        combine = false;
    }

    private void checkTimeout(Long now) throws XAException {
        if (now - branchRegisterTime > TIMEOUT) {
            xaRollback(xaBranchXid);
            throw new XAException("XA branch timeout error");
        }
    }

    @Override
    public void close() throws SQLException {
        try (ResourceLock ignored = resourceLock.obtain()) {
            if (combine) {
                return;
            }
            try {
                if (xaActive && this.xaBranchXid != null) {
                    if (((DataSourceProxyXA) resource).sonataShimEnabled) {
                        sonataPrePrepare();
                    }

                    // XA End: Success
                    try {
                        end(XAResource.TMSUCCESS);
                    } catch (SQLException sqle) {
                        // Rollback immediately before the XA Branch Context is deleted.
                        String xaBranchXid = this.xaBranchXid.toString();
                        rollback();
                        throw new SQLException(
                                "Branch " + xaBranchXid + " was rollbacked on committing since " + sqle.getMessage(),
                                SQLSTATE_XA_NOT_END,
                                sqle);
                    }
                    long now = System.currentTimeMillis();
                    checkTimeout(now);
                    setPrepareTime(now);
                    int prepare = xaResource.prepare(xaBranchXid);
                    // Based on the four databases: MySQL (8), Oracle (12c), Postgres (16), and MSSQL Server (2022),
                    // only Oracle has read-only optimization; the others do not provide read-only feedback.
                    // Therefore, the database type check can be eliminated here.
                    if (prepare == XAResource.XA_RDONLY) {
                        // Branch Report to TC: RDONLY
                        reportStatusToTC(BranchStatus.PhaseOne_RDONLY);
                    }
                }
            } catch (XAException xe) {
                // Some drivers (e.g., PG) do not automatically roll back and reset autocommit when failing to prepare,
                // which would cause the later reuse of the connection to fail at init(). Thus, we do it manually.
                originalConnection.rollback();
                originalConnection.setAutoCommit(true);

                if (((DataSourceProxyXA) resource).sonataShimEnabled) {
                    if (((DataSourceProxyXA) resource).sonataSsiShimEnabled) {
                        try {
                            releaseHelperTxn(xaBranchXid);
                        } catch (XAException ignored2) {
                            // On the RM side, a missing prepared helper txn is only caused by TM-initiated rollback. No
                            // action needed.
                        }
                    }
                    forgetDummyKey(xaBranchXid);
                }

                // Branch Report to TC: Failed
                reportStatusToTC(BranchStatus.PhaseOne_Failed);
                throw new SQLException(
                        "Failed to end(TMSUCCESS)/prepare xa branch on " + xid + "-" + xaBranchXid.getBranchId()
                                + " since " + xe.getMessage(),
                        xe);
            } finally {
                cleanXABranchContext();
                rollBacked = false;
                if (isHeld() && shouldBeHeld()) {
                    // if kept by a keeper, just hold the connection.
                } else {
                    originalConnection.close();
                }
            }
        }
    }

    protected void closeForce() throws SQLException {
        try (ResourceLock ignored = resourceLock.obtain()) {
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
            throw new SQLException("failed xa branch " + xid + " the global transaction has finish, branch status: "
                    + branchStatus.getCode());
        }
    }

    /**
     * Report branch status to TC
     *
     * @param status branch status
     */
    private void reportStatusToTC(BranchStatus status) {
        try {
            DefaultResourceManager.get().branchReport(BranchType.XA, xid, xaBranchXid.getBranchId(), status, null);
        } catch (TransactionException te) {
            LOGGER.warn(
                    "Failed to report XA branch {} on {}-{} since {}:{}",
                    status,
                    xid,
                    xaBranchXid.getBranchId(),
                    te.getCode(),
                    te.getMessage());
        }
    }

    /**
     * Get the lock of the current connection
     * @return the RESOURCE_LOCK
     */
    public ResourceLock getResourceLock() {
        return resourceLock;
    }

    public void setCombine(boolean combine) {
        this.combine = combine;
    }

    private int getS2plDummyKey(XAXid xid) {
        while (true) {
            int key = ThreadLocalRandom.current().nextInt(DUMMY_TABLE_SIZE);
            if (((DataSourceProxyXA) resource).ACTIVE_DUMMY_KEYS.add(key)) {
                Integer prev = ((DataSourceProxyXA) resource).XID_TO_DUMMY_KEY.putIfAbsent(xid, key);
                if (prev != null) {
                    ((DataSourceProxyXA) resource).ACTIVE_DUMMY_KEYS.remove(key);
                    // We don't know why this would happen (though programmatically possible) so we do not handle it.
                    throw new RuntimeException(
                            String.format("Global txn branch (%s) already associated with dummy key (%d)", xid, prev));
                }
                return key;
            }
        }
    }

    private int getSsiDummyKey(XAXid xid) throws SQLException {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        AbstractMap.SimpleEntry<Integer, Integer> keyAndHelperId =
                ((DataSourceProxyXA) resource).RESERVED_DUMMY_KEYS_AND_HELPER_IDS.poll();
        while (keyAndHelperId == null) {
            synchronized (((DataSourceProxyXA) resource).RESERVED_DUMMY_KEYS_AND_HELPER_IDS) {
                if (!((DataSourceProxyXA) resource).RESERVED_DUMMY_KEYS_AND_HELPER_IDS.isEmpty()) {
                    keyAndHelperId = ((DataSourceProxyXA) resource).RESERVED_DUMMY_KEYS_AND_HELPER_IDS.poll();
                    continue;
                }

                // Inside this if block, reserved set is empty and only current thread is making a new one, so new keys
                // won't be added to active set, thus the following contains() test is safe.

                HashSet<Integer> newBatch = new HashSet<>();
                while (newBatch.size() < SSI_HELPER_BATCH_SIZE) {
                    int newKey = random.nextInt(DUMMY_TABLE_SIZE);
                    if (!((DataSourceProxyXA) resource).ACTIVE_DUMMY_KEYS.contains(newKey)) {
                        newBatch.add(newKey);
                    }
                }

                // We should initiate the new helper inside the synchronized block, to guarantee that helper is prepared
                // prior to all corresponding original transactions.

                int helperTxnId;
                AtomicInteger counter = new AtomicInteger(SSI_HELPER_BATCH_SIZE);
                while (true) {
                    helperTxnId = random.nextInt();
                    AtomicInteger prevCounter =
                            ((DataSourceProxyXA) resource).HELPER_ID_REF_COUNT.put(helperTxnId, counter);
                    if (prevCounter == null) {
                        break;
                    }
                }

                try (Connection helperConn = ((DataSourceProxyXA) resource).getSsiHelperConnection()) {
                    helperConn.setAutoCommit(false);

                    try (Statement helperStmt = helperConn.createStatement()) {
                        for (Integer dummyKey : newBatch) {
                            try (ResultSet rs = helperStmt.executeQuery(
                                    "select count(*) from \"" + DUMMY_TABLE + "\" where key=" + dummyKey)) {
                                if (rs.next()) {
                                    rs.getInt(1); // to make sure read is executed in the DB
                                }
                            }
                        }

                        int ret = helperStmt.executeUpdate("prepare transaction '" + helperTxnId + "'");
                        if (ret != 0) {
                            throw new RuntimeException(String.format(
                                    "PostgreSQL txn prepare returned %d, which should be 0 instead", ret));
                        }
                    }
                }

                // Mark them all active
                ((DataSourceProxyXA) resource).ACTIVE_DUMMY_KEYS.addAll(newBatch);

                // Make them visible in random order
                List<Integer> asList = new ArrayList<>(newBatch);
                Collections.shuffle(asList);
                int firstKey = asList.remove(0); // take one for ourselves to avoid loop again
                for (int otherKey : asList) {
                    ((DataSourceProxyXA) resource)
                            .RESERVED_DUMMY_KEYS_AND_HELPER_IDS.add(
                                    new AbstractMap.SimpleEntry<>(otherKey, helperTxnId));
                }

                keyAndHelperId = new AbstractMap.SimpleEntry<>(firstKey, helperTxnId);
            }
        }

        // associate xid with dummy key
        Integer prevDummy = ((DataSourceProxyXA) resource).XID_TO_DUMMY_KEY.putIfAbsent(xid, keyAndHelperId.getKey());
        if (prevDummy != null) {
            throw new RuntimeException(
                    String.format("Global txn branch (%s) already associated with dummy key (%d)", xid, prevDummy));
        }

        // associate xid with helper id
        Integer prevHelper =
                ((DataSourceProxyXA) resource).XID_TO_HELPER_ID.putIfAbsent(xid, keyAndHelperId.getValue());
        if (prevHelper != null) {
            throw new RuntimeException(String.format(
                    "Global txn branch (%s) already associated with helper txn ID (%d)", xid, prevHelper));
        }

        return keyAndHelperId.getKey();
    }

    private void forgetDummyKey(XAXid xid) {
        Integer prev = ((DataSourceProxyXA) resource).XID_TO_DUMMY_KEY.remove(xid);
        if (prev == null) {
            // Possible if TM initiated a rollback, and RM is yet to prepare the branch
            return;
        }

        boolean recorded = ((DataSourceProxyXA) resource).ACTIVE_DUMMY_KEYS.remove(prev);
        if (!recorded) {
            throw new RuntimeException(String.format(
                    "Dummy key (%d) mapped to global txn branch (%s) not found in active dummy key set", prev, xid));
        }
    }

    private boolean releaseHelperTxn(XAXid xid) throws XAException {
        Integer helperTxnId = ((DataSourceProxyXA) resource).XID_TO_HELPER_ID.remove(xid);

        if (helperTxnId == null) {
            // Possible if TM initiated a rollback, and RM is yet to prepare the branch
            return false;
        }

        AtomicInteger counter = ((DataSourceProxyXA) resource).HELPER_ID_REF_COUNT.get(helperTxnId);
        if (counter == null) {
            throw new RuntimeException("Reference counter for helper txn (" + helperTxnId + ") not found");
        }

        int currentUsage = counter.decrementAndGet();
        if (currentUsage == 0) {
            ((DataSourceProxyXA) resource).HELPER_ID_REF_COUNT.remove(helperTxnId);
            try (Statement stmt = originalConnection.createStatement()) { // do not use wrapped xa stmt
                stmt.executeUpdate("rollback prepared '" + helperTxnId + "'");
            } catch (SQLException e) {
                if ("42704".equals(e.getSQLState())) {
                    LOGGER.error("Helper txn ({}) not prepared; should be possible with helper sharing", helperTxnId);
                    // This is caused by TM actively rolling back the branch while the branch is preparing. We return an
                    // XAException to notify TM to try again later. Extremely rare, but still possible (e.g., if helper
                    // batch size=1).
                    XAException xe = new XAException("Helper txn (" + helperTxnId + ") not yet prepared, try again");
                    xe.errorCode = XAException.XA_RETRY;
                    throw xe;
                }

                LOGGER.error("Unexpected SQLException while rolling back helper txn: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private void sonataPrePrepare() throws SQLException, XAException {
        // For an S2PL DB, we add a dummy write; for an SSI DB, we add a helper txn + a dummy write. Then,
        // original prepare logic resumes.
        if (DBType.MYSQL.name().equalsIgnoreCase(resource.getDbType())) {
            int key = getS2plDummyKey(xaBranchXid);
            int value = ThreadLocalRandom.current().nextInt();

            try (Statement stmt = createStatement()) {
                int retry = 0;
                int affected = 0;
                // Note: affected=1 for insert; =2 for update; =0 for update but value unchanged
                while (affected == 0) {
                    affected =
                            stmt.executeUpdate("insert into `" + DUMMY_TABLE + "` (`key`, value) values (" + key + ","
                                    + value + ") on duplicate key update value="
                                    + value);
                    retry++;
                    if (retry > S2PL_UPDATE_RETRY_WARNING_THRESHOLD) {
                        LOGGER.warn(
                                "MySQL random dummy writes generated conflict with existing rows in {} consecutive attempts! This is super unlikely to occur, please investigate.",
                                retry);
                    }
                }
            }
        } else if (DBType.POSTGRESQL.name().equalsIgnoreCase(resource.getDbType())) {
            int key = getSsiDummyKey(xaBranchXid);
            int value = ThreadLocalRandom.current().nextInt();

            try (Statement stmt = createStatement()) {
                // Unlike MySQL, PostgreSQL does not distinguish matched but value-unchanged rows.
                int affected;
                try {
                    affected = stmt.executeUpdate(
                            "insert into \"" + DUMMY_TABLE + "\" (key, value) values (" + key + "," + value
                                    + ") on conflict (key) do update set value="
                                    + value);
                } catch (SQLException e) {
                    if ("40001".equals(e.getSQLState())) {
                        throw new XAException("PostgreSQL dummy write of global txn branch (" + xaBranchXid
                                + ") failed due to serialization failure");
                    }

                    LOGGER.error("Unexpected SQLException while PG dummy write: {}", e.getMessage());
                    throw e;
                }
                if (affected != 1) {
                    // We don't know why this would happen (though programmatically possible) so we do not
                    // handle it.
                    throw new RuntimeException(String.format(
                            "PostgreSQL dummy write affected %d row(s), which should be 1 instead", affected));
                }
            }
        }
    }
}
