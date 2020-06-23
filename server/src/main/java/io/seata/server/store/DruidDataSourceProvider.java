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
package io.seata.server.store;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.filter.config.ConfigFilter;
import com.alibaba.druid.filter.config.ConfigTools;
import com.alibaba.druid.pool.DruidDataSource;

import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.core.store.db.AbstractDataSourceProvider;

/**
 * The druid datasource provider
 * @author zhangsen
 * @author ggndnn
 * @author will
 * @author funkye
 */
@LoadLevel(name = "druid")
public class DruidDataSourceProvider extends AbstractDataSourceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DruidDataSourceProvider.class);

    @Override
    public DataSource generate() {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName(getDriverClassName());
        ds.setDriverClassLoader(getDriverClassLoader());
        ds.setUrl(getUrl());
        ds.setUsername(getUser());
        String publicKey = getPublicKey();
        String password = getPassword();
        if (StringUtils.isNotBlank(publicKey)) {
            try {
                password = ConfigTools.decrypt(publicKey, password);
            } catch (Exception e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(
                        "decryption failed,please confirm whether the ciphertext and secret key are correct! error msg: ",
                        e.getMessage());
                }
            }
        }
        ds.setPassword(password);
        ds.setInitialSize(getMinConn());
        ds.setMaxActive(getMaxConn());
        ds.setMinIdle(getMinConn());
        ds.setMaxWait(getMaxWait());
        ds.setTimeBetweenEvictionRunsMillis(120000);
        ds.setMinEvictableIdleTimeMillis(300000);
        ds.setTestWhileIdle(true);
        ds.setTestOnBorrow(false);
        ds.setPoolPreparedStatements(true);
        ds.setMaxPoolPreparedStatementPerConnectionSize(20);
        ds.setValidationQuery(getValidationQuery(getDBType()));
        ds.setDefaultAutoCommit(true);
        return ds;
    }
}
