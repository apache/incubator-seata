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

import io.seata.common.DefaultValues;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.rpc.Disposable;
import io.seata.rm.tcc.TCCFenceHandler;
import io.seata.rm.tcc.constant.TCCFenceConstant;
import io.seata.rm.tcc.exception.TCCFenceException;
import io.seata.rm.tcc.store.db.TCCFenceStoreDataBaseDAO;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCC Fence Config
 *
 * @author kaka2code
 */
public class TCCFenceConfig implements InitializingBean, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCCFenceConfig.class);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * TCC fence clean period. only d(day)/h(hour)/m(minute) endings are supported
     *
     */
    private String cleanPeriod = DefaultValues.DEFAULT_TCC_FENCE_CLEAN_PERIOD;

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

    public DataSource getDataSource() {
        return dataSource;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setCleanPeriod(String cleanPeriod) {
        this.cleanPeriod = cleanPeriod;
    }

    public void setLogTableName(String logTableName) {
        this.logTableName = logTableName;
    }

    /**
     * init tcc fence clean task
     */
    public void initCleanTask() {
        if (initialized.compareAndSet(false, true)) {
            try {
                String mode = cleanPeriod.substring(cleanPeriod.length() - 1);
                int period = Integer.parseInt(cleanPeriod.substring(0, cleanPeriod.length() - 1));
                if (period == 0) {
                    LOGGER.error("TCC fence log clean period can not be zero, clean task start failed");
                    return;
                }
                // set timeUtil value according to clean mode
                TimeUnit timeUnit;
                if (TCCFenceConstant.DAY.equals(mode)) {
                    timeUnit = TimeUnit.DAYS;
                } else if (TCCFenceConstant.HOUR.equals(mode)) {
                    timeUnit = TimeUnit.HOURS;
                } else if (TCCFenceConstant.MINUTE.equals(mode)) {
                    timeUnit = TimeUnit.MINUTES;
                } else {
                    LOGGER.error("TCC fence log clean period only d/h/m endings are supported, clean task start failed");
                    return;
                }
                // start tcc fence clean schedule
                startTccFenceCleanSchedule(mode, period, timeUnit);
                LOGGER.info("TCC fence log clean task start success, mode:{}, period:{}", mode, period);
            } catch (NumberFormatException e) {
                LOGGER.error("TCC fence log clean period only supports positive integers, clean task start failed");
            }
        }
    }

    /**
     * start tcc fence clean schedule
     * @param mode clean mode
     * @param period clean period
     * @param timeUnit timeunit
     */
    private void startTccFenceCleanSchedule(String mode, int period, TimeUnit timeUnit) {
        tccFenceClean.scheduleWithFixedDelay(() -> {
            Date timeBefore = null;
            try {
                if (TCCFenceConstant.HOUR.equals(mode)) {
                    timeBefore = DateUtils.addHours(new Date(), -period);
                } else if (TCCFenceConstant.MINUTE.equals(mode)) {
                    timeBefore = DateUtils.addMinutes(new Date(), -period);
                } else if (TCCFenceConstant.DAY.equals(mode)) {
                    timeBefore = DateUtils.addDays(new Date(), -period);
                }
                if (timeBefore != null) {
                    int deletedRowCount = TCCFenceHandler.deleteFenceByDate(timeBefore);
                    if (deletedRowCount > 0) {
                        LOGGER.info("TCC fence clean task executed success, timeBefore: {}, deleted row count: {}",
                                timeBefore, deletedRowCount);
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.error("Delete tcc fence log failed, timeBefore: {}", timeBefore, e);
            }
        }, 0, period, timeUnit);
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

