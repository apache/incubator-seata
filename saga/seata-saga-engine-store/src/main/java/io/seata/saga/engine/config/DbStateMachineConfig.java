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
import io.seata.saga.engine.store.db.DBStateLangStore;
import io.seata.saga.engine.store.db.DBStateLogStore;
import io.seata.saga.engine.store.db.TransactionalSqlSessionExecutor;
import io.seata.saga.engine.store.db.MybatisConfig;
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
    private String  tablePrefix                  = "SEATA_";
    private String  transPropagationBehaviorName = "PROPAGATION_REQUIRES_NEW";
    private String  transIsolationLevelName      = "ISOLATION_DEFAULT";
    private String  databaseType                 = "mysql";
    private MybatisConfig             mybatisConfig;
    private SagaTransactionalTemplate sagaTransactionalTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {

        if(mybatisConfig == null){
            mybatisConfig = new MybatisConfig();
            mybatisConfig.setDataSource(dataSource);
            mybatisConfig.setTablePrefix(tablePrefix);
            mybatisConfig.setDatabaseType(databaseType);
            mybatisConfig.setTransPropagationBehaviorName(transPropagationBehaviorName);
            mybatisConfig.setTransIsolationLevelName(transIsolationLevelName);
            mybatisConfig.afterPropertiesSet();
        }

        if(getStateLogStore() == null){
            DBStateLogStore dbStateLogStore = new DBStateLogStore();

            TransactionalSqlSessionExecutor sqlSessionExecutor = new TransactionalSqlSessionExecutor();
            sqlSessionExecutor.setTransactionTemplate(mybatisConfig.getTransactionTemplate());
            sqlSessionExecutor.setSqlSessionTemplate(mybatisConfig.getSqlSessionTemplate());

            dbStateLogStore.setSqlSessionExecutor(sqlSessionExecutor);

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
            DBStateLangStore dbStateLangStore = new DBStateLangStore();
            dbStateLangStore.setSqlSessionTemplate(mybatisConfig.getSqlSessionTemplate());
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

    public void setMybatisConfig(MybatisConfig mybatisConfig) {
        this.mybatisConfig = mybatisConfig;
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

    public String getTransPropagationBehaviorName() {
        return transPropagationBehaviorName;
    }

    public void setTransPropagationBehaviorName(String transPropagationBehaviorName) {
        this.transPropagationBehaviorName = transPropagationBehaviorName;
    }

    public String getTransIsolationLevelName() {
        return transIsolationLevelName;
    }

    public void setTransIsolationLevelName(String transIsolationLevelName) {
        this.transIsolationLevelName = transIsolationLevelName;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }
}