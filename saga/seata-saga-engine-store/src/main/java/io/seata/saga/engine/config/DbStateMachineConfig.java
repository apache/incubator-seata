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
package io.seata.saga.engine.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import io.seata.saga.engine.impl.DefaultStateMachineConfig;
import io.seata.saga.engine.store.db.DbAndReportTcStateLogStore;
import io.seata.saga.engine.store.db.DbStateLangStore;
import io.seata.saga.tm.DefaultSagaTransactionalTemplate;
import io.seata.saga.tm.SagaTransactionalTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

/**
 * DbStateMachineConfig
 *
 * @author lorne.cl
 */
public class DbStateMachineConfig extends DefaultStateMachineConfig implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbStateMachineConfig.class);

    private static final int DEFAULT_TRANS_OPER_TIMEOUT = 60000 * 10;

    private DataSource dataSource;
    private String applicationId;
    private String txServiceGroup;
    private String tablePrefix = "seata_";
    private String dbType;
    private int transOperationTimeout = DEFAULT_TRANS_OPER_TIMEOUT;
    private SagaTransactionalTemplate sagaTransactionalTemplate;

    public static String getDbTypeFromDataSource(DataSource dataSource) throws SQLException {
        Connection con = null;
        try {
            con = dataSource.getConnection();
            DatabaseMetaData metaData = con.getMetaData();
            return metaData.getDatabaseProductName();
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    LOGGER.error("Get dbType from failed: {}", e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        dbType = getDbTypeFromDataSource(dataSource);

        if (getStateLogStore() == null) {
            DbAndReportTcStateLogStore dbStateLogStore = new DbAndReportTcStateLogStore();
            dbStateLogStore.setDataSource(dataSource);
            dbStateLogStore.setTablePrefix(tablePrefix);
            dbStateLogStore.setDbType(dbType);
            dbStateLogStore.setDefaultTenantId(getDefaultTenantId());
            dbStateLogStore.setSeqGenerator(getSeqGenerator());

            if (sagaTransactionalTemplate == null) {
                DefaultSagaTransactionalTemplate defaultSagaTransactionalTemplate
                    = new DefaultSagaTransactionalTemplate();
                defaultSagaTransactionalTemplate.setTimeout(transOperationTimeout);
                defaultSagaTransactionalTemplate.setApplicationContext(getApplicationContext());
                defaultSagaTransactionalTemplate.setApplicationId(applicationId);
                defaultSagaTransactionalTemplate.setTxServiceGroup(txServiceGroup);
                defaultSagaTransactionalTemplate.afterPropertiesSet();
                sagaTransactionalTemplate = defaultSagaTransactionalTemplate;
            }

            dbStateLogStore.setSagaTransactionalTemplate(sagaTransactionalTemplate);

            setStateLogStore(dbStateLogStore);
        }

        if (getStateLangStore() == null) {
            DbStateLangStore dbStateLangStore = new DbStateLangStore();
            dbStateLangStore.setDataSource(dataSource);
            dbStateLangStore.setTablePrefix(tablePrefix);
            dbStateLangStore.setDbType(dbType);

            setStateLangStore(dbStateLangStore);
        }

        super.afterPropertiesSet();//must execute after StateLangStore initialized
    }

    @Override
    public void destroy() throws Exception {
        if ((sagaTransactionalTemplate != null) && (sagaTransactionalTemplate instanceof DisposableBean)) {
            ((DisposableBean)sagaTransactionalTemplate).destroy();
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }

    public void setSagaTransactionalTemplate(SagaTransactionalTemplate sagaTransactionalTemplate) {
        this.sagaTransactionalTemplate = sagaTransactionalTemplate;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public int getTransOperationTimeout() {
        return transOperationTimeout;
    }

    public void setTransOperationTimeout(int transOperationTimeout) {
        this.transOperationTimeout = transOperationTimeout;
    }
}
