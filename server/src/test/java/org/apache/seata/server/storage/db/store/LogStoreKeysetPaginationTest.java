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
package org.apache.seata.server.storage.db.store;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.core.store.LogStore;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.SessionConverter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration and unit tests for composite keyset pagination {@code (gmt_modified, xid)}
 * in {@link LogStoreDataBaseDAO}, {@link DataBaseTransactionStoreManager}, and {@link SessionConverter}.
 */
public class LogStoreKeysetPaginationTest extends BaseSpringBootTest {

    private static BasicDataSource dataSource;
    private static LogStoreDataBaseDAO logStoreDataBaseDAO;
    private static DataBaseTransactionStoreManager storeManager;

    @TempDir
    static Path databaseDirectory;

    @BeforeAll
    public static void start(ApplicationContext context) throws SQLException {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:" + databaseDirectory.resolve("keyset_paging"));
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        prepareTables(dataSource);

        logStoreDataBaseDAO = new LogStoreDataBaseDAO(dataSource);
        logStoreDataBaseDAO.setDbType("h2");
        logStoreDataBaseDAO.setGlobalTable("global_table");
        logStoreDataBaseDAO.setBranchTable("branch_table");

        storeManager = mock(DataBaseTransactionStoreManager.class, CALLS_REAL_METHODS);
        storeManager.setLogStore(logStoreDataBaseDAO);
        storeManager.setLogQueryLimit(100);
    }

    @AfterAll
    public static void stop() throws SQLException {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @BeforeEach
    public void cleanTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM branch_table");
            stmt.execute("DELETE FROM global_table");
        }
        storeManager.setLogStore(logStoreDataBaseDAO);
        storeManager.setLogQueryLimit(100);
    }

    private static void prepareTables(DataSource ds) throws SQLException {
        try (Connection conn = ds.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS global_table");
            stmt.execute("CREATE TABLE global_table ("
                    + "xid VARCHAR(128) PRIMARY KEY, "
                    + "transaction_id BIGINT, "
                    + "status INT NOT NULL, "
                    + "application_id VARCHAR(32), "
                    + "transaction_service_group VARCHAR(32), "
                    + "transaction_name VARCHAR(128), "
                    + "timeout INT, "
                    + "begin_time BIGINT, "
                    + "application_data VARCHAR(500), "
                    + "gmt_create TIMESTAMP(6), "
                    + "gmt_modified TIMESTAMP(6))");
            stmt.execute("DROP TABLE IF EXISTS branch_table");
            stmt.execute("CREATE TABLE branch_table ("
                    + "branch_id BIGINT PRIMARY KEY, "
                    + "xid VARCHAR(128) NOT NULL, "
                    + "transaction_id BIGINT, "
                    + "resource_group_id VARCHAR(32), "
                    + "resource_id VARCHAR(256), "
                    + "branch_type VARCHAR(32), "
                    + "status INT, "
                    + "client_id VARCHAR(64), "
                    + "application_data VARCHAR(500), "
                    + "gmt_create TIMESTAMP(6), "
                    + "gmt_modified TIMESTAMP(6))");
        }
    }

    private void insertGlobalRow(String xid, long txId, GlobalStatus status, Timestamp gmtModified)
            throws SQLException {
        String sql = "INSERT INTO global_table ("
                + "xid, transaction_id, status, application_id, transaction_service_group, "
                + "transaction_name, timeout, begin_time, application_data, gmt_create, gmt_modified"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, xid);
            ps.setLong(2, txId);
            ps.setInt(3, status.getCode());
            ps.setString(4, "app");
            ps.setString(5, "tx-group");
            ps.setString(6, "tx-name");
            ps.setInt(7, 60000);
            ps.setLong(8, gmtModified != null ? gmtModified.getTime() : 1_700_000_000_000L);
            ps.setString(9, "data");
            ps.setTimestamp(10, gmtModified);
            ps.setTimestamp(11, gmtModified);
            ps.executeUpdate();
        }
    }

    private void insertBranchRow(long branchId, String xid, long txId) throws SQLException {
        String sql = "INSERT INTO branch_table ("
                + "branch_id, xid, transaction_id, resource_group_id, resource_id, "
                + "branch_type, status, client_id, application_data, gmt_create, gmt_modified"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, branchId);
            ps.setString(2, xid);
            ps.setLong(3, txId);
            ps.setString(4, "default");
            ps.setString(5, "jdbc:h2:mem:test");
            ps.setString(6, "AT");
            ps.setInt(7, 1);
            ps.setString(8, "client-1");
            ps.setString(9, "branch-data");
            ps.executeUpdate();
        }
    }

    @Test
    public void sameSecondTimestampCollisionsTraverseEveryRowExactlyOnce() throws SQLException {
        Timestamp t0 = Timestamp.valueOf("2026-01-01 10:00:00");
        Timestamp t1 = Timestamp.valueOf("2026-01-01 10:00:05");
        Timestamp t2 = Timestamp.valueOf("2026-01-01 10:00:10");

        // Insert 15 target rows out of order:
        // 4 at T0, 7 colliding at the exact same second T1 (simulating MySQL DATETIME), 4 at T2
        insertGlobalRow("xid-t0-03", 103L, GlobalStatus.Committing, t0);
        insertGlobalRow("xid-t0-01", 101L, GlobalStatus.CommitRetrying, t0);
        insertGlobalRow("xid-t0-04", 104L, GlobalStatus.Committing, t0);
        insertGlobalRow("xid-t0-02", 102L, GlobalStatus.CommitRetrying, t0);

        insertGlobalRow("xid-t1-07", 207L, GlobalStatus.Committing, t1);
        insertGlobalRow("xid-t1-02", 202L, GlobalStatus.CommitRetrying, t1);
        insertGlobalRow("xid-t1-05", 205L, GlobalStatus.Committing, t1);
        insertGlobalRow("xid-t1-01", 201L, GlobalStatus.Committing, t1);
        insertGlobalRow("xid-t1-06", 206L, GlobalStatus.CommitRetrying, t1);
        insertGlobalRow("xid-t1-04", 204L, GlobalStatus.Committing, t1);
        insertGlobalRow("xid-t1-03", 203L, GlobalStatus.CommitRetrying, t1);

        insertGlobalRow("xid-t2-04", 304L, GlobalStatus.CommitRetrying, t2);
        insertGlobalRow("xid-t2-02", 302L, GlobalStatus.Committing, t2);
        insertGlobalRow("xid-t2-01", 301L, GlobalStatus.Committing, t2);
        insertGlobalRow("xid-t2-03", 303L, GlobalStatus.CommitRetrying, t2);

        // Insert non-matching status rows across all timestamps
        insertGlobalRow("xid-other-begin", 401L, GlobalStatus.Begin, t0);
        insertGlobalRow("xid-other-rollback", 402L, GlobalStatus.Rollbacking, t1);
        insertGlobalRow("xid-other-async", 403L, GlobalStatus.AsyncCommitting, t2);

        List<String> expectedOrder = Arrays.asList(
                "xid-t0-01",
                "xid-t0-02",
                "xid-t0-03",
                "xid-t0-04",
                "xid-t1-01",
                "xid-t1-02",
                "xid-t1-03",
                "xid-t1-04",
                "xid-t1-05",
                "xid-t1-06",
                "xid-t1-07",
                "xid-t2-01",
                "xid-t2-02",
                "xid-t2-03",
                "xid-t2-04");

        List<GlobalSession> visited = new ArrayList<>();
        Date cursorGmtModified = null;
        String cursorXid = null;
        int pageCount = 0;

        while (true) {
            SessionCondition condition = new SessionCondition(GlobalStatus.Committing, GlobalStatus.CommitRetrying);
            condition.setLazyLoadBranch(true);
            condition.setPageSize(3);
            condition.setCursorGmtModified(cursorGmtModified);
            condition.setCursorXid(cursorXid);
            assertTrue(condition.isPagedQuery());
            assertEquals(pageCount > 0, condition.hasCursor());

            List<GlobalSession> page = storeManager.readSession(condition);
            if (page.isEmpty()) {
                break;
            }
            pageCount++;
            assertTrue(page.size() <= 3);
            visited.addAll(page);
            GlobalSession last = page.get(page.size() - 1);
            assertNotNull(last.getGmtModified());
            cursorGmtModified = last.getGmtModified();
            cursorXid = last.getXid();
        }

        assertEquals(5, pageCount);
        List<String> actualOrder = visited.stream().map(GlobalSession::getXid).collect(Collectors.toList());
        assertEquals(expectedOrder, actualOrder);
        Set<String> uniqueXids = new HashSet<>(actualOrder);
        assertEquals(15, uniqueXids.size());
    }

    @Test
    public void subSecondTimestampPrecisionIsPreservedAcrossCursorPages() throws SQLException {
        Timestamp ts1 = Timestamp.valueOf("2026-01-01 12:00:00.100000");
        Timestamp ts2 = Timestamp.valueOf("2026-01-01 12:00:00.250500");
        Timestamp ts3 = Timestamp.valueOf("2026-01-01 12:00:00.900123");

        insertGlobalRow("xid-sub-3", 503L, GlobalStatus.Rollbacking, ts2);
        insertGlobalRow("xid-sub-1", 501L, GlobalStatus.Rollbacking, ts1);
        insertGlobalRow("xid-sub-4", 504L, GlobalStatus.Rollbacking, ts3);
        insertGlobalRow("xid-sub-2", 502L, GlobalStatus.Rollbacking, ts2);

        List<String> visited = new ArrayList<>();
        Date cursorTime = null;
        String cursorXid = null;

        while (true) {
            SessionCondition condition = new SessionCondition(GlobalStatus.Rollbacking);
            condition.setPageSize(2);
            condition.setLazyLoadBranch(true);
            condition.setCursorGmtModified(cursorTime);
            condition.setCursorXid(cursorXid);

            List<GlobalSession> page = storeManager.readSession(condition);
            if (page.isEmpty()) {
                break;
            }
            for (GlobalSession session : page) {
                visited.add(session.getXid());
                assertTrue(session.getGmtModified() instanceof Timestamp);
            }
            GlobalSession last = page.get(page.size() - 1);
            cursorTime = last.getGmtModified();
            cursorXid = last.getXid();
        }

        assertEquals(Arrays.asList("xid-sub-1", "xid-sub-2", "xid-sub-3", "xid-sub-4"), visited);
    }

    @Test
    public void pagedQueryRespectsLazyLoadBranchAndBatchLoadsBranches() throws SQLException {
        Timestamp ts = Timestamp.valueOf("2026-01-01 14:00:00");
        insertGlobalRow("xid-br-1", 601L, GlobalStatus.AsyncCommitting, ts);
        insertGlobalRow("xid-br-2", 602L, GlobalStatus.AsyncCommitting, ts);
        insertBranchRow(6001L, "xid-br-1", 601L);
        insertBranchRow(6002L, "xid-br-1", 601L);
        insertBranchRow(6003L, "xid-br-2", 602L);

        LogStore spyStore = spy(logStoreDataBaseDAO);
        storeManager.setLogStore(spyStore);

        // lazyLoadBranch = false -> batch load branches in a single query
        SessionCondition eager = new SessionCondition(GlobalStatus.AsyncCommitting);
        eager.setPageSize(10);
        eager.setLazyLoadBranch(false);
        List<GlobalSession> eagerSessions = storeManager.readSession(eager);

        assertEquals(2, eagerSessions.size());
        assertFalse(eagerSessions.get(0).isLazyLoadBranch());
        assertEquals(2, eagerSessions.get(0).getBranchSessions().size());
        assertEquals(1, eagerSessions.get(1).getBranchSessions().size());
        verify(spyStore, times(1)).queryBranchTransactionDO(anyList());

        clearInvocations(spyStore);

        // lazyLoadBranch = true -> do not query branch table
        SessionCondition lazy = new SessionCondition(GlobalStatus.AsyncCommitting);
        lazy.setPageSize(10);
        lazy.setLazyLoadBranch(true);
        List<GlobalSession> lazySessions = storeManager.readSession(lazy);

        assertEquals(2, lazySessions.size());
        assertTrue(lazySessions.get(0).isLazyLoadBranch());
        assertTrue(lazySessions.get(1).isLazyLoadBranch());
        verify(spyStore, never()).queryBranchTransactionDO(anyList());
        verify(spyStore, never()).queryBranchTransactionDO(anyString());
    }

    @Test
    public void unsetPaginationFieldsUseLegacyPathAndNonPositivePageSizeFallsBackToLogQueryLimit() {
        DataBaseTransactionStoreManager manager = mock(DataBaseTransactionStoreManager.class, CALLS_REAL_METHODS);
        LogStore mockStore = mock(LogStore.class);
        manager.setLogStore(mockStore);
        manager.setLogQueryLimit(42);

        when(mockStore.queryGlobalTransactionDO(any(int[].class), anyInt())).thenReturn(Collections.emptyList());
        when(mockStore.queryGlobalTransactionDO(any(int[].class), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        // No pagination fields set -> exact legacy path
        SessionCondition legacy = new SessionCondition(GlobalStatus.Committing);
        assertFalse(legacy.isPagedQuery());
        assertFalse(legacy.hasCursor());
        manager.readSession(legacy);
        verify(mockStore, times(1)).queryGlobalTransactionDO(new int[] {GlobalStatus.Committing.getCode()}, 42);
        verify(mockStore, never()).queryGlobalTransactionDO(any(int[].class), any(), any(), anyInt());

        clearInvocations(mockStore);

        // pageSize <= 0 -> falls back to logQueryLimit while still using paged query
        SessionCondition fallbackLimit = new SessionCondition(GlobalStatus.Committing);
        fallbackLimit.setPageSize(0);
        assertTrue(fallbackLimit.isPagedQuery());
        manager.readSession(fallbackLimit);
        verify(mockStore, times(1))
                .queryGlobalTransactionDO(new int[] {GlobalStatus.Committing.getCode()}, null, null, 42);

        clearInvocations(mockStore);

        // Cursor set with null pageSize -> falls back to logQueryLimit
        Date cursor = new Date(1_700_000_000_000L);
        SessionCondition cursorOnly = new SessionCondition(GlobalStatus.Committing);
        cursorOnly.setCursorGmtModified(cursor);
        cursorOnly.setCursorXid("xid-1");
        assertTrue(cursorOnly.hasCursor());
        manager.readSession(cursorOnly);
        verify(mockStore, times(1))
                .queryGlobalTransactionDO(new int[] {GlobalStatus.Committing.getCode()}, cursor, "xid-1", 42);
    }

    @Test
    public void logStoreDefaultMethodFallsBackToTwoArgQuery() {
        List<GlobalTransactionDO> expected = Collections.singletonList(new GlobalTransactionDO("xid-default"));
        LogStore customLogStore = new LogStore() {
            @Override
            public GlobalTransactionDO queryGlobalTransactionDO(String xid) {
                return null;
            }

            @Override
            public GlobalTransactionDO queryGlobalTransactionDO(long transactionId) {
                return null;
            }

            @Override
            public List<GlobalTransactionDO> queryGlobalTransactionDO(int[] status, int limit) {
                assertArrayEquals(new int[] {2}, status);
                assertEquals(10, limit);
                return expected;
            }

            @Override
            public boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
                return false;
            }

            @Override
            public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
                return false;
            }

            @Override
            public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO, Integer expectedStatus) {
                return false;
            }

            @Override
            public boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
                return false;
            }

            @Override
            public List<BranchTransactionDO> queryBranchTransactionDO(String xid) {
                return Collections.emptyList();
            }

            @Override
            public List<BranchTransactionDO> queryBranchTransactionDO(List<String> xids) {
                return Collections.emptyList();
            }

            @Override
            public boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
                return false;
            }

            @Override
            public boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
                return false;
            }

            @Override
            public boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
                return false;
            }

            @Override
            public long getCurrentMaxSessionId(long high, long low) {
                return 0;
            }
        };

        assertEquals(expected, customLogStore.queryGlobalTransactionDO(new int[] {2}, new Date(), "xid-0", 10));
    }

    @Test
    public void sessionConverterPreservesGmtModifiedWithoutChangingGlobalSessionCodec() {
        Timestamp ts = Timestamp.valueOf("2026-02-03 04:05:06.123456");
        GlobalTransactionDO globalDO = new GlobalTransactionDO();
        globalDO.setXid("127.0.0.1:8091:999");
        globalDO.setTransactionId(999L);
        globalDO.setStatus(GlobalStatus.Committing.getCode());
        globalDO.setApplicationId("app");
        globalDO.setTransactionServiceGroup("group");
        globalDO.setTransactionName("tx");
        globalDO.setTimeout(60000);
        globalDO.setBeginTime(1_700_000_000_000L);
        globalDO.setApplicationData("payload");
        globalDO.setGmtModified(ts);

        GlobalSession withGmtModified = SessionConverter.convertGlobalSession(globalDO, false);
        assertEquals(ts, withGmtModified.getGmtModified());

        GlobalSession lazyWithGmtModified = SessionConverter.convertGlobalSession(globalDO, true);
        assertEquals(ts, lazyWithGmtModified.getGmtModified());

        globalDO.setGmtModified(null);
        GlobalSession withoutGmtModified = SessionConverter.convertGlobalSession(globalDO, false);
        assertArrayEquals(withoutGmtModified.encode(), withGmtModified.encode());
    }

    @Test
    public void nullGmtModifiedRowsAreSkippedWithoutStallingPagination() throws SQLException {
        Timestamp t1 = Timestamp.valueOf("2026-01-01 11:00:00");
        Timestamp t2 = Timestamp.valueOf("2026-01-01 11:00:05");

        insertGlobalRow("xid-null-gmt", 700L, GlobalStatus.Committing, null);
        insertGlobalRow("xid-valid-01", 701L, GlobalStatus.Committing, t1);
        insertGlobalRow("xid-valid-02", 702L, GlobalStatus.Committing, t1);
        insertGlobalRow("xid-valid-03", 703L, GlobalStatus.Committing, t2);
        insertGlobalRow("xid-valid-04", 704L, GlobalStatus.Committing, t2);
        insertGlobalRow("xid-valid-05", 705L, GlobalStatus.Committing, t2);

        List<String> visited = new ArrayList<>();
        Date cursorTime = null;
        String cursorXid = null;
        int iterations = 0;

        while (true) {
            iterations++;
            assertTrue(iterations <= 10, "Pagination must not loop indefinitely when NULL gmt_modified rows exist");
            SessionCondition condition = new SessionCondition(GlobalStatus.Committing);
            condition.setPageSize(2);
            condition.setLazyLoadBranch(true);
            condition.setCursorGmtModified(cursorTime);
            condition.setCursorXid(cursorXid);

            List<GlobalSession> page = storeManager.readSession(condition);
            if (page.isEmpty()) {
                break;
            }
            for (GlobalSession session : page) {
                assertNotNull(session.getGmtModified());
                visited.add(session.getXid());
            }
            GlobalSession last = page.get(page.size() - 1);
            cursorTime = last.getGmtModified();
            cursorXid = last.getXid();
        }

        assertEquals(
                Arrays.asList("xid-valid-01", "xid-valid-02", "xid-valid-03", "xid-valid-04", "xid-valid-05"), visited);
    }

    @Test
    public void daoGuardsInvalidInputsAndRejectsPartialCursor() throws Exception {
        DataSource mockDs = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(mockDs.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getTables(any(), any(), any(), any())).thenReturn(mock(ResultSet.class));
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        LogStoreDataBaseDAO dao = new LogStoreDataBaseDAO(mockDs);
        dao.setDbType("mysql");
        clearInvocations(mockDs, conn, ps, rs);

        assertTrue(dao.queryGlobalTransactionDO(null, null, null, 10).isEmpty());
        assertTrue(dao.queryGlobalTransactionDO(new int[0], null, null, 10).isEmpty());
        assertTrue(dao.queryGlobalTransactionDO(new int[] {1}, null, null, 0).isEmpty());
        assertTrue(dao.queryGlobalTransactionDO(new int[] {1}, null, null, -5).isEmpty());
        verify(mockDs, never()).getConnection();

        // Partial cursors must fail fast with IllegalArgumentException without hitting the DB
        assertThrows(
                IllegalArgumentException.class,
                () -> dao.queryGlobalTransactionDO(new int[] {2}, new Date(), null, 10));
        assertThrows(
                IllegalArgumentException.class,
                () -> dao.queryGlobalTransactionDO(new int[] {2}, new Date(), "   ", 10));
        assertThrows(
                IllegalArgumentException.class, () -> dao.queryGlobalTransactionDO(new int[] {2}, null, "xid-1", 10));
        verify(mockDs, never()).getConnection();

        // DataBaseTransactionStoreManager routes partial cursor to DAO (fail-fast) rather than legacy path
        DataBaseTransactionStoreManager manager = mock(DataBaseTransactionStoreManager.class, CALLS_REAL_METHODS);
        manager.setLogStore(dao);
        manager.setLogQueryLimit(10);
        SessionCondition partial = new SessionCondition(GlobalStatus.Committing);
        partial.setCursorXid("xid-only");
        assertTrue(partial.isDbPagedQuery());
        assertTrue(partial.hasPartialCursor());
        assertFalse(partial.hasCursor());
        assertFalse(partial.isRedisPagedQuery());
        assertThrows(IllegalArgumentException.class, () -> manager.readSession(partial));

        // Offset-only condition is Redis-only, so DB takes legacy path
        SessionCondition offsetOnly = new SessionCondition(GlobalStatus.Committing);
        offsetOnly.setOffset(5);
        assertFalse(offsetOnly.isDbPagedQuery());
        assertTrue(offsetOnly.isRedisPagedQuery());
        assertTrue(offsetOnly.isPagedQuery());
    }

    @Test
    public void sqlServerAndStandardDialectsBindParametersInExactPositionalOrder() throws Exception {
        DataSource mockDs = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(mockDs.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getTables(any(), any(), any(), any())).thenReturn(mock(ResultSet.class));
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        LogStoreDataBaseDAO dao = new LogStoreDataBaseDAO(mockDs);
        Timestamp cursorTs = Timestamp.valueOf("2026-03-04 05:06:07.654321");
        int[] statuses = new int[] {GlobalStatus.Committing.getCode(), GlobalStatus.CommitRetrying.getCode()};

        // 1. SQL Server ordered first page: top (?) first, then statuses
        dao.setDbType("sqlserver");
        clearInvocations(conn, ps);
        dao.queryGlobalTransactionDO(statuses, null, null, 25);

        verify(conn).prepareStatement(contains("select top (?) "));
        InOrder sqlServerFirstPage = inOrder(ps);
        sqlServerFirstPage.verify(ps).setInt(1, 25);
        sqlServerFirstPage.verify(ps).setInt(2, GlobalStatus.Committing.getCode());
        sqlServerFirstPage.verify(ps).setInt(3, GlobalStatus.CommitRetrying.getCode());
        sqlServerFirstPage.verify(ps).executeQuery();

        // 2. SQL Server cursor page: top (?) first, then statuses, then cursorTs, cursorTs, cursorXid
        clearInvocations(conn, ps);
        dao.queryGlobalTransactionDO(statuses, cursorTs, "xid-cursor", 25);

        verify(conn).prepareStatement(contains("(gmt_modified > ? or (gmt_modified = ? and xid > ?))"));
        InOrder sqlServerCursor = inOrder(ps);
        sqlServerCursor.verify(ps).setInt(1, 25);
        sqlServerCursor.verify(ps).setInt(2, GlobalStatus.Committing.getCode());
        sqlServerCursor.verify(ps).setInt(3, GlobalStatus.CommitRetrying.getCode());
        sqlServerCursor.verify(ps).setTimestamp(4, cursorTs);
        sqlServerCursor.verify(ps).setTimestamp(5, cursorTs);
        sqlServerCursor.verify(ps).setString(6, "xid-cursor");
        sqlServerCursor.verify(ps).executeQuery();

        // 3. Standard dialect (mysql) cursor page: statuses, cursorTs, cursorTs, cursorXid, limit last
        dao.setDbType("mysql");
        clearInvocations(conn, ps);
        dao.queryGlobalTransactionDO(statuses, cursorTs, "xid-cursor", 25);

        InOrder mysqlCursor = inOrder(ps);
        mysqlCursor.verify(ps).setInt(1, GlobalStatus.Committing.getCode());
        mysqlCursor.verify(ps).setInt(2, GlobalStatus.CommitRetrying.getCode());
        mysqlCursor.verify(ps).setTimestamp(3, cursorTs);
        mysqlCursor.verify(ps).setTimestamp(4, cursorTs);
        mysqlCursor.verify(ps).setString(5, "xid-cursor");
        mysqlCursor.verify(ps).setInt(6, 25);
        mysqlCursor.verify(ps).executeQuery();
    }
}
