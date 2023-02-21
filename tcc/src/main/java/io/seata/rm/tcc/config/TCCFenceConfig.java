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

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import io.seata.common.DefaultValues;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.rpc.Disposable;
import io.seata.rm.tcc.TCCFenceHandler;
import io.seata.rm.tcc.exception.TCCFenceException;
import io.seata.rm.tcc.store.db.TCCFenceStoreDataBaseDAO;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * TCC Fence Config
 *
 * @author kaka2code
 */
public class TCCFenceConfig implements InitializingBean, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCFenceConfig.class);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * TCC fence clean period max value. maximum interval is 68 years
     */
    private static final Duration MAX_PERIOD = Duration.ofSeconds(Integer.MAX_VALUE);

    /**
     * TCC fence clean period. only duration type format are supported
     */
    private Duration cleanPeriod = Duration.ofDays(DefaultValues.DEFAULT_TCC_FENCE_CLEAN_PERIOD);

    /**
     * TCC fence log table name
     */
    private String logTableName = DefaultValues.DEFAULT_TCC_FENCE_LOG_TABLE_NAME;

    /**
     * TCC fence datasource
     */
    private final DataSource dataSource;

    /**
     * TCC fence transactionManager
     */
    private final PlatformTransactionManager transactionManager;

    /**
     * TCC fence clean scheduled thread pool
     */
    private final ScheduledThreadPoolExecutor tccFenceClean = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("tccFenceClean", 1));

    public TCCFenceConfig(final DataSource dataSource, final PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    public AtomicBoolean getInitialized() {
        return initialized;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setCleanPeriod(Duration cleanPeriod) {
        this.cleanPeriod = cleanPeriod;
    }

    public void setLogTableName(String logTableName) {
        this.logTableName = logTableName;
    }

    /**
     * init tcc fence clean task
     */
    public void initCleanTask() {
        try {
            // disable clear task when cleanPeriod <= 0
            if (cleanPeriod.isZero() || cleanPeriod.isNegative()) {
                LOGGER.info("TCC fence log clean task is not started, cleanPeriod is:{}", cleanPeriod);
                return;
            }
            // convert to second level. maximum interval is 68 years
            long periodSeconds = cleanPeriod.compareTo(MAX_PERIOD) >= 0 ? Integer.MAX_VALUE : cleanPeriod.toMillis() / 1000;
            // start tcc fence clean schedule
            tccFenceClean.scheduleWithFixedDelay(() -> {
                Date timeBefore = null;
                try {
                    timeBefore = DateUtils.addSeconds(new Date(), -(int)periodSeconds);
                    int deletedRowCount = TCCFenceHandler.deleteFenceByDate(timeBefore);
                    if (deletedRowCount > 0) {
                        LOGGER.info("TCC fence clean task executed success, timeBefore: {}, deleted row count: {}",
                                timeBefore, deletedRowCount);
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("Delete tcc fence log failed, timeBefore: {}", timeBefore, e);
                }
            }, 0, periodSeconds, TimeUnit.SECONDS);
            LOGGER.info("TCC fence log clean task start success, cleanPeriod:{}", cleanPeriod);
        } catch (NumberFormatException e) {
            LOGGER.error("TCC fence log clean period only supports positive integers, clean task start failed");
        }
    }

    @Override
    public void destroy() {
        // shutdown delete tcc fence log task
        tccFenceClean.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        // set log table name
        if (logTableName != null) {
            TCCFenceStoreDataBaseDAO.getInstance().setLogTableName(logTableName);
        }
        if (dataSource != null) {
            // set dataSource
            TCCFenceHandler.setDataSource(dataSource);
        } else {
            throw new TCCFenceException(FrameworkErrorCode.DateSourceNeedInjected);
        }
        if (transactionManager != null) {
            // set transaction template
            TCCFenceHandler.setTransactionTemplate(new TransactionTemplate(transactionManager));
        } else {
            throw new TCCFenceException(FrameworkErrorCode.TransactionManagerNeedInjected);
        }
    }
}

