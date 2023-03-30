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
package io.seata.integration.tx.api.fence.config;

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.seata.common.DefaultValues;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.rpc.Disposable;
import io.seata.integration.tx.api.fence.DefaultCommonFenceHandler;
import io.seata.integration.tx.api.fence.store.db.CommonFenceStoreDataBaseDAO;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common Fence Config
 *
 * @author kaka2code
 */
public class CommonFenceConfig implements Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonFenceConfig.class);

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Common fence clean period max value. maximum interval is 68 years
     */
    private static final Duration MAX_PERIOD = Duration.ofSeconds(Integer.MAX_VALUE);

    /**
     * Common fence clean period. only duration type format are supported
     */
    private Duration cleanPeriod = Duration.ofDays(DefaultValues.DEFAULT_COMMON_FENCE_CLEAN_PERIOD);

    /**
     * Common fence log table name
     */
    private String logTableName = DefaultValues.DEFAULT_COMMON_FENCE_LOG_TABLE_NAME;


    /**
     * Common fence clean scheduled thread pool
     */
    private final ScheduledThreadPoolExecutor tccFenceClean = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("tccFenceClean", 1));

    public AtomicBoolean getInitialized() {
        return initialized;
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
                    timeBefore = DateUtils.addSeconds(new Date(), -(int) periodSeconds);
                    int deletedRowCount = DefaultCommonFenceHandler.get().deleteFenceByDate(timeBefore);
                    if (deletedRowCount > 0) {
                        LOGGER.info("TCC fence clean task executed success, timeBefore: {}, deleted row count: {}",
                                timeBefore, deletedRowCount);
                    }
                } catch (RuntimeException e) {
                    LOGGER.error("Delete tcc fence log failed, timeBefore: {}", timeBefore, e);
                }
            },  new Random(System.currentTimeMillis()).nextInt(60), periodSeconds, TimeUnit.SECONDS);

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

    public void init() {
        // set log table name
        if (logTableName != null) {
            CommonFenceStoreDataBaseDAO.getInstance().setLogTableName(logTableName);
        }
        initCleanTask();
    }


}
