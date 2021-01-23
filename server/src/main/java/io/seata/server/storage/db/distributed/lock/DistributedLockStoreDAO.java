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
package io.seata.server.storage.db.distributed.lock;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import com.alibaba.druid.util.JdbcUtils;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.DistributedLockDO;
import io.seata.core.store.DistributedLockStore;
import io.seata.core.store.db.sql.distribute.lock.DistributeLockSqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.DefaultValues.DEFAULT_DISTRIBUTE_LOCK_DB_TABLE;
import static io.seata.core.constants.ConfigurationKeys.DISTRIBUTE_LOCK_DB_TABLE;

/**
 * @author chd
 */
public class DistributedLockStoreDAO implements DistributedLockStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockStoreDAO.class);

    private DataSource distributedLockDataSource;

    private String distributeLockTable;

    private String dbType;

    /**
     * Instantiates a new Log store data base dao.
     *
     * @param distributedLockDataSource the distribute lock store data source
     */
    public DistributedLockStoreDAO(DataSource distributedLockDataSource) {
        Configuration configuration = ConfigurationFactory.getInstance();
        distributeLockTable = configuration.getConfig(DISTRIBUTE_LOCK_DB_TABLE, DEFAULT_DISTRIBUTE_LOCK_DB_TABLE);
        dbType = configuration.getConfig(ConfigurationKeys.STORE_DB_TYPE);

        this.distributedLockDataSource = distributedLockDataSource;
    }


    @Override
    public boolean acquireLock(DistributedLockDO distributedLockDO) {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = distributedLockDataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            DistributedLockDO distributedLockDOFromDB = getDistributeLockDO(connection, distributedLockDO.getKey());
            if (null == distributedLockDOFromDB) {
                return insertDistribute(connection, distributedLockDO);
            }

            if (distributedLockDOFromDB.getExpire() >= System.currentTimeMillis()) {
                LOGGER.info("the distribute lock for key :{} is holding by :{}, acquire lock failure.",
                        distributedLockDO.getKey(), distributedLockDOFromDB.getValue());
                return false;
            }

            return updateDistributeLock(connection, distributedLockDO);
        } catch (SQLException ex) {
            LOGGER.error("execute acquire lock failure, key is: {}", distributedLockDO.getKey(), ex);
            return false;
        } finally {
            try {
                if (originalAutoCommit) {
                    connection.commit();
                    connection.setAutoCommit(true);
                }
                JdbcUtils.close(connection);
            } catch (SQLException ignore) { }
        }
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = distributedLockDataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            DistributedLockDO distributedLockDOFromDB = getDistributeLockDO(connection, distributedLockDO.getKey());
            if (null == distributedLockDOFromDB) {
                throw new ShouldNeverHappenException("distributeLockDO would not be null when release distribute lock");
            }

            if (distributedLockDOFromDB.getExpire() >= System.currentTimeMillis()
                    && !Objects.equals(distributedLockDOFromDB.getValue(), distributedLockDO.getValue())) {
                LOGGER.warn("the distribute lock for key :{} is holding by :{}, skip the release lock.",
                        distributedLockDO.getKey(), distributedLockDOFromDB.getValue());
                return true;
            }

            return updateDistributeLock(connection, distributedLockDO);
        } catch (SQLException ex) {
            LOGGER.error("execute release lock failure, key is: {}", distributedLockDO.getKey(), ex);
            return false;
        } finally {
            try {
                if (originalAutoCommit) {
                    connection.commit();
                    connection.setAutoCommit(true);
                }
                JdbcUtils.close(connection);
            } catch (SQLException ignore) { }
        }
    }

    protected DistributedLockDO getDistributeLockDO(Connection connection, String key) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType)
                .getSelectDistributeForUpdateSql(distributeLockTable))) {

            pst.setString(1, key);
            ResultSet resultSet = pst.executeQuery();

            while (resultSet.next()) {
                DistributedLockDO distributedLock = new DistributedLockDO();
                distributedLock.setExpire(resultSet.getLong(ServerTableColumnsName.DISTRIBUTE_LOCK_EXPIRE));
                distributedLock.setValue(resultSet.getString(ServerTableColumnsName.DISTRIBUTE_LOCK_VALUE));
                distributedLock.setKey(key);
                return distributedLock;
            }
            return null;
        }
    }

    protected boolean insertDistribute(Connection connection, DistributedLockDO distributedLockDO) throws SQLException {
        try (PreparedStatement insertPst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType)
                .getInsertSql(distributeLockTable))){
            insertPst.setString(1, distributedLockDO.getKey());
            insertPst.setString(2, distributedLockDO.getValue());
            insertPst.setLong(3, distributedLockDO.getExpire());
            return insertPst.executeUpdate() > 0;
        }
    }

    protected boolean updateDistributeLock(Connection connection, DistributedLockDO distributedLockDO) throws SQLException {
        try (PreparedStatement updatePst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType)
                .getUpdateSql(distributeLockTable))) {
            updatePst.setString(1, distributedLockDO.getValue());
            updatePst.setLong(2, distributedLockDO.getExpire());
            updatePst.setString(3, distributedLockDO.getKey());
            return updatePst.executeUpdate() > 0;
        }
    }
}
