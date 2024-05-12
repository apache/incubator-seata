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
package io.seata.saga.engine.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import io.seata.saga.engine.impl.DefaultStateMachineConfig;
import io.seata.saga.engine.store.impl.StateLogStoreImpl;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
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
 * The type Db state machine config.
 */
@Deprecated
public class DbStateMachineConfig extends DefaultStateMachineConfig implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbStateMachineConfig.class);

    private DataSource dataSource;
    private String applicationId;
    private String txServiceGroup;
    private String accessKey;
    private String secretKey;
    private String tablePrefix = "seata_";
    private String dbType;
    private boolean rmReportSuccessEnable = DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
    private boolean sagaBranchRegisterEnable = DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;

    private SagaTransactionalTemplate sagaTransactionalTemplate;

    /**
     * Instantiates a new Db state machine config.
     */
    public DbStateMachineConfig() {
        try {
            Configuration configuration = ConfigurationFactory.getInstance();
            if (configuration != null) {
                this.rmReportSuccessEnable = configuration.getBoolean(ConfigurationKeys.CLIENT_REPORT_SUCCESS_ENABLE, DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE);
                this.sagaBranchRegisterEnable = configuration.getBoolean(ConfigurationKeys.CLIENT_SAGA_BRANCH_REGISTER_ENABLE, DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE);
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

    /**
     * Gets db type from data source.
     *
     * @param dataSource the data source
     * @return the db type from data source
     * @throws SQLException the sql exception
     */
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

            setStateLogStore(StateLogStoreImpl.wrap(dbStateLogStore));
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

    /**
     * Gets data source.
     *
     * @return the data source
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets data source.
     *
     * @param dataSource the data source
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Gets application id.
     *
     * @return the application id
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets application id.
     *
     * @param applicationId the application id
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets tx service group.
     *
     * @return the tx service group
     */
    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    /**
     * Sets tx service group.
     *
     * @param txServiceGroup the tx service group
     */
    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }

    /**
     * Gets access key.
     *
     * @return the access key
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Sets access key.
     *
     * @param accessKey the access key
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Gets secret key.
     *
     * @return the secret key
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Sets secret key.
     *
     * @param secretKey the secret key
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Sets saga transactional template.
     *
     * @param sagaTransactionalTemplate the saga transactional template
     */
    public void setSagaTransactionalTemplate(SagaTransactionalTemplate sagaTransactionalTemplate) {
        this.sagaTransactionalTemplate = sagaTransactionalTemplate;
    }

    /**
     * Gets table prefix.
     *
     * @return the table prefix
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * Sets table prefix.
     *
     * @param tablePrefix the table prefix
     */
    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    /**
     * Gets db type.
     *
     * @return the db type
     */
    public String getDbType() {
        return dbType;
    }

    /**
     * Sets db type.
     *
     * @param dbType the db type
     */
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    /**
     * Is rm report success enable boolean.
     *
     * @return the boolean
     */
    public boolean isRmReportSuccessEnable() {
        return rmReportSuccessEnable;
    }

    /**
     * Is saga branch register enable boolean.
     *
     * @return the boolean
     */
    public boolean isSagaBranchRegisterEnable() {
        return sagaBranchRegisterEnable;
    }

    /**
     * Sets rm report success enable.
     *
     * @param rmReportSuccessEnable the rm report success enable
     */
    public void setRmReportSuccessEnable(boolean rmReportSuccessEnable) {
        this.rmReportSuccessEnable = rmReportSuccessEnable;
    }

    /**
     * Sets saga branch register enable.
     *
     * @param sagaBranchRegisterEnable the saga branch register enable
     */
    public void setSagaBranchRegisterEnable(boolean sagaBranchRegisterEnable) {
        this.sagaBranchRegisterEnable = sagaBranchRegisterEnable;
    }
}
