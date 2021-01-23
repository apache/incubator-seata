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
        try (Connection connection = distributedLockDataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType).getSelectDistributeForUpdateSql(distributeLockTable));
             PreparedStatement insertPst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType).getInsertOnDuplicateKeySql(distributeLockTable))){
            pst.setString(1, distributedLockDO.getKey());
            ResultSet resultSet = pst.executeQuery();
            while (resultSet.next()) {
                long expire = resultSet.getLong(ServerTableColumnsName.DISTRIBUTE_LOCK_EXPIRE);
                String value = resultSet.getString(ServerTableColumnsName.DISTRIBUTE_LOCK_VALUE);
                if (expire >= System.currentTimeMillis()) {
                    LOGGER.info("the distribute lock for key :{} is holding by :{}, acquire lock failure.",
                            distributedLockDO.getKey(), value);
                    return false;
                }
            }

            insertPst.setString(1, distributedLockDO.getKey());
            insertPst.setString(2, distributedLockDO.getValue());
            insertPst.setLong(3, distributedLockDO.getExpire());
            insertPst.setString(4, distributedLockDO.getValue());
            insertPst.setLong(5, distributedLockDO.getExpire());

            return insertPst.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.error("execute acquire lock failure, key is: {}", distributedLockDO.getKey(), ex);
            return false;
        }
    }

    @Override
    public boolean releaseLock(DistributedLockDO distributedLockDO) {
        try (Connection connection = distributedLockDataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType).getSelectDistributeForUpdateSql(distributeLockTable));
             PreparedStatement insertPst = connection.prepareStatement(DistributeLockSqlFactory.getDistributeLogStoreSql(dbType).getInsertOnDuplicateKeySql(distributeLockTable))){
            pst.setString(1, distributedLockDO.getKey());
            ResultSet resultSet = pst.executeQuery();
            while (resultSet.next()) {
                long expire = resultSet.getLong(ServerTableColumnsName.DISTRIBUTE_LOCK_EXPIRE);
                String value = resultSet.getString(ServerTableColumnsName.DISTRIBUTE_LOCK_VALUE);

                if (expire >= System.currentTimeMillis() && Objects.equals(value, distributedLockDO.getValue())) {
                    LOGGER.warn("the distribute lock for key :{} is holding by :{}, skip the release lock.",
                            distributedLockDO.getKey(), value);
                    return true;
                }
            }

            insertPst.setString(1, distributedLockDO.getKey());
            insertPst.setString(2, distributedLockDO.getValue());
            insertPst.setLong(3, distributedLockDO.getExpire());
            insertPst.setString(4, distributedLockDO.getValue());
            insertPst.setLong(5, distributedLockDO.getExpire());

            return insertPst.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.error("execute release lock failure, key is: {}", distributedLockDO.getKey(), ex);
            return false;
        }
    }
}
