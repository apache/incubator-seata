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

import io.seata.saga.engine.impl.DefaultStateMachineConfig;
import io.seata.saga.engine.store.db.DbStateLangStore;
import io.seata.saga.engine.store.db.DbAndReportTcStateLogStore;
import io.seata.saga.tm.DefaultSagaTransactionalTemplate;
import io.seata.saga.tm.SagaTransactionalTemplate;
import org.springframework.beans.factory.DisposableBean;

import javax.sql.DataSource;

/**
 * DbStateMachineConfig
 *
 * @author lorne.cl
 */
public class DbStateMachineConfig extends DefaultStateMachineConfig implements DisposableBean {

    private DataSource                dataSource;
    private String                    applicationId;
    private String                    txServiceGroup;
    private String                    tablePrefix                  = "seata_";
    private String                    dbType                       = "mysql";
    private SagaTransactionalTemplate sagaTransactionalTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {

        if(getStateLogStore() == null){
            DbAndReportTcStateLogStore dbStateLogStore = new DbAndReportTcStateLogStore();
            dbStateLogStore.setDataSource(dataSource);
            dbStateLogStore.setTablePrefix(tablePrefix);
            dbStateLogStore.setDbType(dbType);
            dbStateLogStore.setDefaultTenantId(getDefaultTenantId());

            if(sagaTransactionalTemplate == null){
                DefaultSagaTransactionalTemplate defaultSagaTransactionalTemplate = new DefaultSagaTransactionalTemplate();
                defaultSagaTransactionalTemplate.setApplicationContext(getApplicationContext());
                defaultSagaTransactionalTemplate.setApplicationId(applicationId);
                defaultSagaTransactionalTemplate.setTxServiceGroup(txServiceGroup);
                defaultSagaTransactionalTemplate.afterPropertiesSet();
                sagaTransactionalTemplate = defaultSagaTransactionalTemplate;
            }

            dbStateLogStore.setSagaTransactionalTemplate(sagaTransactionalTemplate);

            setStateLogStore(dbStateLogStore);
        }

        if(getStateLangStore() == null){
            DbStateLangStore dbStateLangStore = new DbStateLangStore();
            dbStateLangStore.setDataSource(dataSource);
            dbStateLangStore.setTablePrefix(tablePrefix);
            dbStateLangStore.setDbType(dbType);

            setStateLangStore(dbStateLangStore);
        }

        super.afterPropertiesSet();
    }

    @Override
    public void destroy() throws Exception {
        if((sagaTransactionalTemplate != null) && (sagaTransactionalTemplate instanceof DisposableBean)){
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
}