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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.saga.engine.impl.DefaultStateMachineConfig;
import io.seata.saga.engine.serializer.impl.ParamsSerializer;
import io.seata.saga.engine.store.db.DbAndReportTcStateLogStore;
import io.seata.saga.engine.store.db.DbStateLangStore;
import io.seata.saga.tm.DefaultSagaTransactionalTemplate;
import io.seata.saga.tm.SagaTransactionalTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

import static io.seata.common.DefaultValues.DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
import static io.seata.common.DefaultValues.DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;
import static io.seata.common.DefaultValues.DEFAULT_SAGA_JSON_PARSER;

/**
 * DbStateMachineConfig
 *
 * @author lorne.cl
 */
public class DbStateMachineConfig extends DefaultStateMachineConfig implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbStateMachineConfig.class);

    private DataSource dataSource;
    private String applicationId;
    private String txServiceGroup;
    private String tablePrefix = "seata_";
    private String dbType;
    private SagaTransactionalTemplate sagaTransactionalTemplate;
    private boolean rmReportSuccessEnable = DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE;
    private boolean sagaBranchRegisterEnable = DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE;
    private String sagaJsonParser = DEFAULT_SAGA_JSON_PARSER;


    public DbStateMachineConfig() {
        try {
            Configuration configuration = ConfigurationFactory.getInstance();
            if (configuration != null) {
                this.rmReportSuccessEnable = configuration.getBoolean(ConfigurationKeys.CLIENT_REPORT_SUCCESS_ENABLE, DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE);
                this.sagaBranchRegisterEnable = configuration.getBoolean(ConfigurationKeys.CLIENT_SAGA_BRANCH_REGISTER_ENABLE, DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE);
                this.sagaJsonParser = configuration.getConfig(ConfigurationKeys.CLIENT_SAGA_JSON_PARSER, DEFAULT_SAGA_JSON_PARSER);
                this.applicationId = configuration.getConfig(ConfigurationKeys.APPLICATION_ID);
                this.txServiceGroup = configuration.getConfig(ConfigurationKeys.TX_SERVICE_GROUP);
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

        dbType = getDbTypeFromDataSource(dataSource);

        if (getStateLogStore() == null) {
            DbAndReportTcStateLogStore dbStateLogStore = new DbAndReportTcStateLogStore();
            dbStateLogStore.setDataSource(dataSource);
            dbStateLogStore.setTablePrefix(tablePrefix);
            dbStateLogStore.setDbType(dbType);
            dbStateLogStore.setDefaultTenantId(getDefaultTenantId());
            dbStateLogStore.setSeqGenerator(getSeqGenerator());

            if (StringUtils.hasLength(sagaJsonParser)) {
                ParamsSerializer paramsSerializer = new ParamsSerializer();
                paramsSerializer.setJsonParserName(sagaJsonParser);
                dbStateLogStore.setParamsSerializer(paramsSerializer);
            }

            if (sagaTransactionalTemplate == null) {
                DefaultSagaTransactionalTemplate defaultSagaTransactionalTemplate
                    = new DefaultSagaTransactionalTemplate();
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

    public boolean isRmReportSuccessEnable() {
        return rmReportSuccessEnable;
    }

    public boolean isSagaBranchRegisterEnable() {
        return sagaBranchRegisterEnable;
    }

    public void setSagaBranchRegisterEnable(boolean sagaBranchRegisterEnable) {
        this.sagaBranchRegisterEnable = sagaBranchRegisterEnable;
    }

    public void setRmReportSuccessEnable(boolean rmReportSuccessEnable) {
        this.rmReportSuccessEnable = rmReportSuccessEnable;
    }

    public String getSagaJsonParser() {
        return sagaJsonParser;
    }

    public void setSagaJsonParser(String sagaJsonParser) {
        this.sagaJsonParser = sagaJsonParser;
    }
}
