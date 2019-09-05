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
package io.seata.saga.engine.store.db;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

/**
 * Mybatis configurations
 * @author lorne.cl
 */
public class MybatisConfig implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisConfig.class);

    public static final String MAPPER_PREFIX = MybatisConfig.class.getName() + ".";

    private static final String TABLE_PREFIX_KEY             = "TABLE_PREFIX";
    private static final String DEFAULT_MYBATIS_MAPPING_FILE = "mybatis/mappings.xml";

    private TransactionTemplate        transactionTemplate;
    private PlatformTransactionManager transactionManager;
    private SqlSessionFactory          sqlSessionFactory;
    private TransactionFactory         transactionFactory;
    private SqlSessionTemplate         sqlSessionTemplate;
    private DataSource                 dataSource;
    private String  transPropagationBehaviorName = "PROPAGATION_REQUIRES_NEW";
    private String  transIsolationLevelName      = "ISOLATION_DEFAULT";
    private String  tablePrefix                  = "SEATA_";
    private String  databaseType                 = "mysql";
    private boolean initTable                    = false;

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() throws Exception {
        initTransactionFactory();
        initSqlSessionFactory();
        initSqlSessionTemplate();

        initTransactionManager();
        initTransactionTemplate();
    }

    public void initSqlSessionFactory() {
        if (sqlSessionFactory == null || !sqlSessionFactory.getConfiguration().getMappedStatementNames().contains(
                MAPPER_PREFIX + "recordStateMachineStarted")) {
            InputStream inputStream = null;
            try {
                inputStream = getMyBatisXmlConfigurationSteam();
                Environment environment = new Environment("default", transactionFactory, dataSource);
                Reader reader = new InputStreamReader(inputStream);
                Properties properties = new Properties();
                properties.setProperty(TABLE_PREFIX_KEY, tablePrefix);
                XMLConfigBuilder parser = new XMLConfigBuilder(reader, "", properties);
                Configuration configuration = parser.getConfiguration();
                configuration.setEnvironment(environment);
                configuration.setDatabaseId(databaseType);
                configuration = parser.parse();
                sqlSessionFactory = new DefaultSqlSessionFactory(configuration);
            } catch (Exception e) {
                LOGGER.error("Init sqlSessionFactory failed" + e.getMessage(), e);
            } finally {
                closeSilently(inputStream);
            }
        }
    }

    public void initTransactionFactory() {
        transactionFactory = new SpringManagedTransactionFactory();
    }

    public void initSqlSessionTemplate() {
        sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory);
    }

    public void initTransactionManager(){
        transactionManager = new DataSourceTransactionManager(dataSource);
    }

    public void initTransactionTemplate() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehaviorName(transPropagationBehaviorName);
        transactionTemplate.setIsolationLevelName(transIsolationLevelName);
    }

    public InputStream getMyBatisXmlConfigurationSteam() {
        return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
    }

    public InputStream getResourceAsStream(String name) {
        InputStream resourceStream;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        resourceStream = classLoader.getResourceAsStream(name);
        return resourceStream;
    }

    public void closeSilently(InputStream inputStream) {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException ignore) {
            LOGGER.warn("close inputStream error", ignore);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
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

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public boolean isInitTable() {
        return initTable;
    }

    public void setInitTable(boolean initTable) {
        this.initTable = initTable;
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public TransactionFactory getTransactionFactory() {
        return transactionFactory;
    }

    public void setTransactionFactory(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    public SqlSessionTemplate getSqlSessionTemplate() {
        return sqlSessionTemplate;
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}