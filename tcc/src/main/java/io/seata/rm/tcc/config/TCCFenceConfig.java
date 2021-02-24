package io.seata.rm.tcc.config;

import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.DateUtils;
import io.seata.core.rpc.Disposable;
import io.seata.rm.tcc.TCCFenceHandler;
import io.seata.rm.tcc.constant.TCCFenceCleanMode;
import io.seata.rm.tcc.constant.TCCFenceConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * TCC fence clean mode, clean up by days by default
     */
    private TCCFenceCleanMode cleanMode = TCCFenceCleanMode.Default;

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
     * TCC fence clean scheduled thread pool
     */
    private ScheduledThreadPoolExecutor tccFenceClean = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("tccFenceClean", 1));

    public TCCFenceConfig() {
    }

    public TCCFenceConfig(TCCFenceCleanMode cleanMode, int cleanPeriod) {
        this.cleanMode = cleanMode;
        this.cleanPeriod = cleanPeriod;
        // init tcc fence clean task
        initCleanTask();
    }

    public TCCFenceConfig(DataSource dataSource, TCCFenceCleanMode cleanMode, int cleanPeriod) {
        this.cleanMode = cleanMode;
        this.cleanPeriod = cleanPeriod;
        TCCFenceConfig.dataSource = dataSource;
        // init tcc fence clean task
        initCleanTask();
    }

    public TCCFenceConfig(Builder builder) {
        this.cleanMode = builder.cleanMode;
        this.cleanPeriod = builder.cleanPeriod;
        TCCFenceConfig.logTableName = builder.logTableName;
        TCCFenceConfig.dataSource = builder.dataSource;
        // init tcc fence clean task
        initCleanTask();
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        TCCFenceConfig.dataSource = dataSource;
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
     * init tcc fence clean task
     */
    private void initCleanTask() {
        if (!cleanMode.equals(TCCFenceCleanMode.Close)) {
            // Set timeUtil value and cleanPeriod field according to clean mode
            TimeUnit timeUnit = null;
            Date timeBefore = null;
            if (cleanMode.equals(TCCFenceCleanMode.Day) || cleanMode.equals(TCCFenceCleanMode.Default)) {
                timeUnit = TimeUnit.DAYS;
                if (cleanPeriod == 0) {
                    cleanPeriod = TCCFenceConstant.DEFAULT_CLEAN_DAY;
                }
                timeBefore = DateUtils.getDayBefore(cleanPeriod);
            } else if (cleanMode.equals(TCCFenceCleanMode.Hour)) {
                timeUnit = TimeUnit.HOURS;
                if (cleanPeriod == 0) {
                    cleanPeriod = TCCFenceConstant.DEFAULT_CLEAN_HOUR;
                }
                timeBefore = DateUtils.getHourBefore(cleanPeriod);
            }

            Date finalTimeBefore = timeBefore;
            tccFenceClean.scheduleAtFixedRate(() -> {
                boolean result = TCCFenceHandler.deleteFenceByDate(finalTimeBefore);
                if (!result) {
                    LOGGER.error("TCC fence clean task executed error!");
                }
                LOGGER.info("TCC fence clean task executed success!");
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
        private TCCFenceCleanMode cleanMode = TCCFenceCleanMode.Default;
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

