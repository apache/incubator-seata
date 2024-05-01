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
package org.apache.seata.saga.engine.config;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.saga.engine.impl.DefaultStateMachineConfig;
import org.apache.seata.saga.engine.serializer.impl.ParamsSerializer;
import org.apache.seata.saga.engine.store.db.DbAndReportTcStateLogStore;
import org.apache.seata.saga.engine.store.db.DbStateLangStore;
import org.apache.seata.saga.engine.tm.DefaultSagaTransactionalTemplate;
import org.apache.seata.saga.engine.tm.SagaTransactionalTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE;
import static org.apache.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE;
import static org.apache.seata.common.DefaultValues.DEFAULT_SAGA_JSON_PARSER;

/**
 * DbStateMachineConfig
 */
public class DbStateMachineConfig extends DefaultStateMachineConfig implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbStateMachineConfig.class);

    private DataSource dataSource;
    private String applicationId;
    private String txServiceGroup;
    private String accessKey;
    private String secretKey;
    private String tablePrefix = "seata_";
    private String dbType;
    private SagaTransactionalTemplate sagaTransactionalTemplate;

    public DbStateMachineConfig() {
        try {
            Configuration configuration = ConfigurationFactory.getInstance();
            if (configuration != null) {
                setRmReportSuccessEnable(configuration.getBoolean(ConfigurationKeys.CLIENT_REPORT_SUCCESS_ENABLE, DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE));
                setSagaBranchRegisterEnable(configuration.getBoolean(ConfigurationKeys.CLIENT_SAGA_BRANCH_REGISTER_ENABLE, DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE));
                setSagaJsonParser(configuration.getConfig(ConfigurationKeys.CLIENT_SAGA_JSON_PARSER, DEFAULT_SAGA_JSON_PARSER));
                this.applicationId = configuration.getConfig(ConfigurationKeys.APPLICATION_ID);
                this.txServiceGroup = configuration.getConfig(ConfigurationKeys.TX_SERVICE_GROUP);
                this.accessKey = configuration.getConfig(ConfigurationKeys.ACCESS_KEY, null);
                this.secretKey = configuration.getConfig(ConfigurationKeys.SECRET_KEY, null);
                setSagaRetryPersistModeUpdate(configuration.getBoolean(ConfigurationKeys.CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE,
                        DEFAULT_CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE));
                setSagaCompensatePersistModeUpdate(configuration.getBoolean(ConfigurationKeys.CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE,
                        DEFAULT_CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE));
            }
        } catch (Exception e) {
            LOGGER.warn("Load SEATA configuration failed, use default configuration instead.", e);
        }
    }

    public static String getDbTypeFromDataSource(DataSource dataSource) throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            DatabaseMetaData metaData = con.getMetaData();
            return metaData.getDatabaseProductName();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (dataSource == null) {
            throw new IllegalArgumentException("datasource required not null!");
        }

        dbType = getDbTypeFromDataSource(dataSource);
        if (getStateLogStore() == null) {
            DbAndReportTcStateLogStore dbStateLogStore = new DbAndReportTcStateLogStore();
            dbStateLogStore.setDataSource(dataSource);
            dbStateLogStore.setTablePrefix(tablePrefix);
            dbStateLogStore.setDbType(dbType);
            dbStateLogStore.setDefaultTenantId(getDefaultTenantId());
            dbStateLogStore.setSeqGenerator(getSeqGenerator());

            if (StringUtils.hasLength(getSagaJsonParser())) {
                ParamsSerializer paramsSerializer = new ParamsSerializer();
                paramsSerializer.setJsonParserName(getSagaJsonParser());
                dbStateLogStore.setParamsSerializer(paramsSerializer);
            }

            if (sagaTransactionalTemplate == null) {
                DefaultSagaTransactionalTemplate defaultSagaTransactionalTemplate = new DefaultSagaTransactionalTemplate();
                defaultSagaTransactionalTemplate.setApplicationContext(getApplicationContext());
                defaultSagaTransactionalTemplate.setApplicationId(applicationId);
                defaultSagaTransactionalTemplate.setTxServiceGroup(txServiceGroup);
                defaultSagaTransactionalTemplate.setAccessKey(accessKey);
                defaultSagaTransactionalTemplate.setSecretKey(secretKey);
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

        //must execute after StateLangStore initialized
        super.afterPropertiesSet();
    }

    @Override
    public void destroy() throws Exception {
        if ((sagaTransactionalTemplate != null) && (sagaTransactionalTemplate instanceof DisposableBean)) {
            ((DisposableBean) sagaTransactionalTemplate).destroy();
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

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
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
}
