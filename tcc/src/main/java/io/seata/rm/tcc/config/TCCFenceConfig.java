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
package io.seata.rm.tcc.config;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.DateUtils;
import io.seata.core.rpc.Disposable;
import io.seata.rm.tcc.TCCFenceHandler;
import io.seata.rm.tcc.constant.TCCFenceCleanMode;
import io.seata.rm.tcc.constant.TCCFenceConstant;
import io.seata.rm.tcc.exception.TCCFenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * TCC Fence Config
 *
 * @author cebbank
 */
public class TCCFenceConfig implements Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCFenceConfig.class);

    /**
     * TCC fence clean mode
     */
    private TCCFenceCleanMode cleanMode;

    /**
     * TCC fence clean period
     */
    private int cleanPeriod;

    /**
     * TCC fence log table name
     */
    private static String logTableName = TCCFenceConstant.DEFAULT_LOG_TABLE_NAME;

    /**
     * TCC fence datasource
     */
    private static DataSource dataSource;

    /**
     * Spring transaction template
     */
    private static TransactionTemplate transactionTemplate;

    /**
     * TCC fence clean scheduled thread pool
     */
    private ScheduledThreadPoolExecutor tccFenceClean = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("tccFenceClean", 1));

    public TCCFenceConfig(DataSource dataSource, TCCFenceCleanMode cleanMode, int cleanPeriod) {
        this.cleanMode = cleanMode;
        this.cleanPeriod = cleanPeriod;
        TCCFenceConfig.dataSource = dataSource;
        // init transaction template
        initTransTemplate();
        // init tcc fence clean task
        initCleanTask();
    }

    public TCCFenceConfig(Builder builder) {
        this.cleanMode = builder.cleanMode;
        this.cleanPeriod = builder.cleanPeriod;
        TCCFenceConfig.logTableName = builder.logTableName;
        TCCFenceConfig.dataSource = builder.dataSource;
        initTransTemplate();
        // init tcc fence clean task
        initCleanTask();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        TCCFenceConfig.dataSource = dataSource;
    }

    public static TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        TCCFenceConfig.transactionTemplate = transactionTemplate;
    }

    public TCCFenceCleanMode getCleanMode() {
        return cleanMode;
    }

    public void setCleanMode(TCCFenceCleanMode cleanMode) {
        this.cleanMode = cleanMode;
    }

    public int getCleanPeriod() {
        return cleanPeriod;
    }

    public void setCleanPeriod(int cleanPeriod) {
        this.cleanPeriod = cleanPeriod;
    }

    public static String getLogTableName() {
        return logTableName;
    }

    public void setLogTableName(String logTableName) {
        TCCFenceConfig.logTableName = logTableName;
    }

    /**
     * init transaction template
     */
    private void initTransTemplate() {
        if (dataSource != null) {
            PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            TCCFenceConfig.transactionTemplate = new TransactionTemplate(transactionManager);
        } else {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
    }
    /**
     * init tcc fence clean task
     */
    private void initCleanTask() {
        if (!TCCFenceCleanMode.Close.equals(cleanMode)) {
            // Set timeUtil value and cleanPeriod field according to clean mode
            TimeUnit timeUnit;
            if (TCCFenceCleanMode.Hour.equals(cleanMode)) {
                timeUnit = TimeUnit.HOURS;
                if (cleanPeriod == 0) {
                    cleanPeriod = TCCFenceConstant.DEFAULT_CLEAN_HOUR;
                }
            } else if (TCCFenceCleanMode.Minute.equals(cleanMode)) {
                timeUnit = TimeUnit.MINUTES;
                if (cleanPeriod == 0) {
                    cleanPeriod = TCCFenceConstant.DEFAULT_CLEAN_MINUTE;
                }
            } else {
                // Default or clean mode equals TCCFenceCleanMode.Day
                timeUnit = TimeUnit.DAYS;
                if (cleanPeriod == 0) {
                    cleanPeriod = TCCFenceConstant.DEFAULT_CLEAN_DAY;
                }
            }
            tccFenceClean.scheduleAtFixedRate(() -> {
                Date timeBefore;
                if (TCCFenceCleanMode.Hour.equals(cleanMode)) {
                    timeBefore = DateUtils.getHourBefore(cleanPeriod);
                } else if (TCCFenceCleanMode.Minute.equals(cleanMode)) {
                    timeBefore = DateUtils.getMinuteBefore(cleanPeriod);
                } else {
                    timeBefore = DateUtils.getDayBefore(cleanPeriod);
                }
                boolean result = TCCFenceHandler.deleteFenceByDate(timeBefore);
                if (!result) {
                    LOGGER.error("TCC fence clean task executed error!");
                }
                LOGGER.info("TCC fence clean task executed success! timeBefore: {}", timeBefore);
            }, 0, cleanPeriod, timeUnit);
        }
    }

    @Override
    public void destroy() {
        // shutdown delete tcc fence log task
        tccFenceClean.shutdown();
    }

    /**
     * TCC fence config builder
     */
    public static class Builder {
        private TCCFenceCleanMode cleanMode = TCCFenceCleanMode.Day;
        private int cleanPeriod = 0;
        private String logTableName = TCCFenceConstant.DEFAULT_LOG_TABLE_NAME;
        private DataSource dataSource = null;

        public Builder cleanMode(TCCFenceCleanMode val) {
            cleanMode = val;
            return this;
        }
        public Builder cleanPeriod(int val) {
            cleanPeriod = val;
            return this;
        }
        public Builder dateSource(DataSource val) {
            dataSource = val;
            return this;
        }
        public Builder logTableName(String val) {
            logTableName = val;
            return this;
        }
        public TCCFenceConfig build() {
            return new TCCFenceConfig(this);
        }
    }
}

