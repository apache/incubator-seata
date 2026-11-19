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
package org.apache.seata.server.storage.db.lock;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.CachedConfigurationChangeListener;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.constants.ServerTableColumnsName;
import org.apache.seata.core.store.DistributedLockDO;
import org.apache.seata.core.store.DistributedLocker;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.core.store.db.sql.distributed.lock.DistributedLockSql;
import org.apache.seata.core.store.db.sql.distributed.lock.DistributedLockSqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.apache.seata.core.constants.ConfigurationKeys.DISTRIBUTED_LOCK_DB_NOWAIT_ENABLED;
import static org.apache.seata.core.constants.ConfigurationKeys.DISTRIBUTED_LOCK_DB_TABLE;

/**
 */
@LoadLevel(name = "db", scope = Scope.SINGLETON)
public class DataBaseDistributedLocker implements DistributedLocker {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataBaseDistributedLocker.class);

    private final String dbType;

    private final String datasourceType;

    private volatile String distributedLockTable;

    private DataSource distributedLockDataSource;

    private static final String LOCK_WAIT_TIMEOUT_MYSQL_MESSAGE = "try restarting transaction";

    private static final int LOCK_WAIT_TIMEOUT_MYSQL_CODE = 1205;

    /**
     * MySQL 8.0+: ER_LOCK_NOWAIT.
     * Statement aborted because lock(s) could not be acquired immediately and NOWAIT is set.
     */
    private static final int LOCK_NOWAIT_MYSQL_CODE = 3572;

    /**
     * Oracle: ORA-00054 - resource busy and acquire with NOWAIT specified or timeout expired.
     * The driver maps the ORA-XXXXX number to the JDBC error code, so we match on 54.
     */
    private static final int LOCK_NOWAIT_ORACLE_CODE = 54;

    /**
     * PostgreSQL: 55P03 lock_not_available, raised when {@code FOR UPDATE NOWAIT} hits a held row.
     */
    private static final String LOCK_NOWAIT_POSTGRESQL_SQLSTATE = "55P03";

    private static final Set<Integer> IGNORE_MYSQL_CODE = new HashSet<>();

    private static final Set<String> IGNORE_MYSQL_MESSAGE = new HashSet<>();

    /**
     * Vendor error codes that signal a NOWAIT fast-fail rather than a real failure.
     */
    private static final Set<Integer> IGNORE_NOWAIT_CODE = new HashSet<>();

    /**
     * SQLState values that signal a NOWAIT fast-fail rather than a real failure.
     */
    private static final Set<String> IGNORE_NOWAIT_SQLSTATE = new HashSet<>();

    static {
        IGNORE_MYSQL_CODE.add(LOCK_WAIT_TIMEOUT_MYSQL_CODE);
        IGNORE_MYSQL_MESSAGE.add(LOCK_WAIT_TIMEOUT_MYSQL_MESSAGE);
        IGNORE_NOWAIT_CODE.add(LOCK_NOWAIT_MYSQL_CODE);
        IGNORE_NOWAIT_CODE.add(LOCK_NOWAIT_ORACLE_CODE);
        IGNORE_NOWAIT_SQLSTATE.add(LOCK_NOWAIT_POSTGRESQL_SQLSTATE);
    }

    /**
     * whether NOWAIT acquisition is enabled. Resolved once in the constructor and
     * propagated through configuration listener so toggling at runtime is supported.
     */
    private volatile boolean nowaitEnabled;

    /**
     * whether the distribute lock demotion
     * using for 1.5.0 only and will remove in 1.6.0
     */
    @Deprecated
    private volatile boolean demotion;

    /**
     * Instantiates a new Log store data base dao.
     */
    public DataBaseDistributedLocker() {
        Configuration configuration = ConfigurationFactory.getInstance();

        distributedLockTable = configuration.getConfig(DISTRIBUTED_LOCK_DB_TABLE);
        dbType = configuration.getConfig(ConfigurationKeys.STORE_DB_TYPE);
        datasourceType = configuration.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        nowaitEnabled = configuration.getBoolean(
                DISTRIBUTED_LOCK_DB_NOWAIT_ENABLED, DefaultValues.DEFAULT_DISTRIBUTED_LOCK_DB_NOWAIT_ENABLED);
        configuration.addConfigListener(DISTRIBUTED_LOCK_DB_NOWAIT_ENABLED, new CachedConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                String newValue = event.getNewValue();
                if (StringUtils.isNotBlank(newValue)) {
                    nowaitEnabled = Boolean.parseBoolean(newValue);
                }
            }
        });

        if (StringUtils.isBlank(distributedLockTable)) {
            demotion = true;
            configuration.addConfigListener(DISTRIBUTED_LOCK_DB_TABLE, new CachedConfigurationChangeListener() {
                @Override
                public void onChangeEvent(ConfigurationChangeEvent event) {
                    String newValue = event.getNewValue();
                    if (StringUtils.isNotBlank(newValue)) {
                        distributedLockTable = newValue;
                        init();
                        demotion = false;
                        ConfigurationFactory.getInstance().removeConfigListener(DISTRIBUTED_LOCK_DB_TABLE, this);
                    }
                }
            });

            LOGGER.error("The distribute lock table is not config, please create the target table and config it");
            return;
        }

        init();
    }

    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        if (demotion) {
            return true;
        }

        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = distributedLockDataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            DistributedLockDO lockFromDB = getDistributedLockDO(connection, distributedLockDO.getLockKey());
            if (null == lockFromDB) {
                boolean ret = insertDistribute(connection, distributedLockDO);
                connection.commit();
                return ret;
            }

            if (lockFromDB.getExpireTime() >= System.currentTimeMillis()) {
                LOGGER.debug(
                        "the distribute lock for key :{} is holding by :{}, acquire lock failure.",
                        distributedLockDO.getLockKey(),
                        lockFromDB.getLockValue());
                connection.commit();
                return false;
            }

            boolean ret = updateDistributedLock(connection, distributedLockDO);
            connection.commit();

            return ret;
        } catch (SQLException ex) {
            // ignore "Lock wait timeout exceeded" (legacy blocking) and the
            // NOWAIT fast-fail signals (MySQL 3572 / Oracle ORA-00054 /
            // PostgreSQL 55P03). Other SQLExceptions are unexpected and logged.
            if (!ignoreSQLException(ex)) {
                LOGGER.error("execute acquire lock failure, key is: {}", distributedLockDO.getLockKey(), ex);
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "fast-fail acquiring distribute lock for key :{} due to contention (errorCode={}, sqlState={})",
                        distributedLockDO.getLockKey(),
                        ex.getErrorCode(),
                        ex.getSQLState());
            }
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e) {
                LOGGER.warn("rollback fail because of {}", e.getMessage(), e);
            }
            return false;
        } finally {
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(true);
                }
                IOUtil.close(connection);
            } catch (SQLException ignore) {
            }
        }
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        if (demotion) {
            return true;
        }

        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = distributedLockDataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            DistributedLockDO distributedLockDOFromDB =
                    getDistributedLockDO(connection, distributedLockDO.getLockKey());
            if (null == distributedLockDOFromDB) {
                throw new ShouldNeverHappenException(
                        "distributedLockDO would not be null when release distribute lock");
            }

            if (distributedLockDOFromDB.getExpireTime() >= System.currentTimeMillis()
                    && !Objects.equals(distributedLockDOFromDB.getLockValue(), distributedLockDO.getLockValue())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "the distribute lock for key :{} is holding by :{}, skip the release lock.",
                            distributedLockDO.getLockKey(),
                            distributedLockDOFromDB.getLockValue());
                }
                connection.commit();
                return true;
            }

            distributedLockDO.setLockValue(StringUtils.SPACE);
            distributedLockDO.setExpireTime(0L);
            boolean ret = updateDistributedLock(connection, distributedLockDO);

            connection.commit();
            return ret;
        } catch (SQLException ex) {
            if (!ignoreSQLException(ex)) {
                LOGGER.error("execute release lock failure, key is: {}", distributedLockDO.getLockKey(), ex);
            }

            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e) {
                LOGGER.warn("rollback fail because of {}", e.getMessage(), e);
            }
            return false;
        } finally {
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(true);
                }
                IOUtil.close(connection);
            } catch (SQLException ignore) {
            }
        }
    }

    protected DistributedLockDO getDistributedLockDO(Connection connection, String key) throws SQLException {
        DistributedLockSql lockSql = DistributedLockSqlFactory.getDistributedLogStoreSql(dbType);
        String selectSql = nowaitEnabled
                ? lockSql.getSelectDistributeForUpdateNoWaitSql(distributedLockTable)
                : lockSql.getSelectDistributeForUpdateSql(distributedLockTable);
        try (PreparedStatement pst = connection.prepareStatement(selectSql)) {

            pst.setString(1, key);
            ResultSet resultSet = pst.executeQuery();

            if (resultSet.next()) {
                DistributedLockDO distributedLock = new DistributedLockDO();
                distributedLock.setExpireTime(resultSet.getLong(ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE));
                distributedLock.setLockValue(resultSet.getString(ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE));
                distributedLock.setLockKey(key);
                return distributedLock;
            }
            return null;
        }
    }

    protected boolean insertDistribute(Connection connection, DistributedLockDO distributedLockDO) throws SQLException {
        try (PreparedStatement insertPst = connection.prepareStatement(
                DistributedLockSqlFactory.getDistributedLogStoreSql(dbType).getInsertSql(distributedLockTable))) {
            insertPst.setString(1, distributedLockDO.getLockKey());
            insertPst.setString(2, distributedLockDO.getLockValue());
            if (distributedLockDO.getExpireTime() > 0) {
                distributedLockDO.setExpireTime(distributedLockDO.getExpireTime() + System.currentTimeMillis());
            }
            insertPst.setLong(3, distributedLockDO.getExpireTime());
            return insertPst.executeUpdate() > 0;
        }
    }

    protected boolean updateDistributedLock(Connection connection, DistributedLockDO distributedLockDO)
            throws SQLException {
        try (PreparedStatement updatePst = connection.prepareStatement(
                DistributedLockSqlFactory.getDistributedLogStoreSql(dbType).getUpdateSql(distributedLockTable))) {
            updatePst.setString(1, distributedLockDO.getLockValue());
            if (distributedLockDO.getExpireTime() > 0) {
                distributedLockDO.setExpireTime(distributedLockDO.getExpireTime() + System.currentTimeMillis());
            }
            updatePst.setLong(2, distributedLockDO.getExpireTime());
            updatePst.setString(3, distributedLockDO.getLockKey());
            return updatePst.executeUpdate() > 0;
        }
    }

    private void init() {
        this.distributedLockDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType)
                .provide();
    }

    private boolean ignoreSQLException(SQLException exception) {
        if (IGNORE_MYSQL_CODE.contains(exception.getErrorCode())) {
            return true;
        }
        if (IGNORE_NOWAIT_CODE.contains(exception.getErrorCode())) {
            return true;
        }
        if (StringUtils.isNotBlank(exception.getSQLState())
                && IGNORE_NOWAIT_SQLSTATE.contains(exception.getSQLState())) {
            return true;
        }
        if (StringUtils.isNotBlank(exception.getMessage())) {
            return IGNORE_MYSQL_MESSAGE.stream()
                    .anyMatch(message -> exception.getMessage().contains(message));
        }
        return false;
    }
}
