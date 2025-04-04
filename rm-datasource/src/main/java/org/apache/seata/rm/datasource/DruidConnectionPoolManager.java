package org.apache.seata.rm.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.seata.core.model.Resource;
import org.apache.seata.rm.datasource.entity.ConnectionPoolMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DruidConnectionPoolManager extends AbstractConnectionPoolManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DruidConnectionPoolManager.class);

    private final Map<String, DruidDataSource> druidDataSourceMap = new ConcurrentHashMap<>();

    private static DruidConnectionPoolManager INSTANCE;

    public static DruidConnectionPoolManager getInstance() {
        if (INSTANCE == null) {
            synchronized (DruidConnectionPoolManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DruidConnectionPoolManager();
                }
            }
        }
        return INSTANCE;
    }

    private DruidConnectionPoolManager() {

    }

    public void registerDataSource(Resource resource) {
        if (resource instanceof DataSourceProxy) {
            DataSourceProxy dataSourceProxy = (DataSourceProxy) resource;
            druidDataSourceMap.put(dataSourceProxy.getResourceId(), (DruidDataSource) dataSourceProxy.getTargetDataSource());
        }
    }

    public DruidDataSource get(String resourceId) {
        return druidDataSourceMap.get(resourceId);
    }

    public List<ConnectionPoolMetrics> getConnectionPoolMetricsList() {
        List<ConnectionPoolMetrics> metricsList = new ArrayList<>();

        druidDataSourceMap.forEach((resourceId, druidDataSource) -> {
            ConnectionPoolMetrics metrics = getConnectionPoolMetrics(resourceId);
            metricsList.add(metrics);
        });

        return metricsList;
    }

    public ConnectionPoolMetrics getConnectionPoolMetrics(String resourceId) {
        DruidDataSource druidDataSource = druidDataSourceMap.get(resourceId);
        if (druidDataSource == null) {
            return null;
        }

        ConnectionPoolMetrics metrics = new ConnectionPoolMetrics();
        metrics.setResourceId(resourceId);
        metrics.setCurrentTimeMillis(System.currentTimeMillis());
        metrics.setActiveCount(druidDataSource.getActiveCount());
        metrics.setMaxActive(druidDataSource.getMaxActive());
        metrics.setIdleCount(druidDataSource.getPoolingCount());

        return metrics;
    }

    public void adjustPoolConfig(String resourceId, int maxActive, int minIdle) {
        DruidDataSource druidDataSource = druidDataSourceMap.get(resourceId);
        if (druidDataSource == null) {
            return;
        }
        if (maxActive >= 0) {
            druidDataSource.setMaxActive(maxActive);
        } else if (minIdle >= 0) {
            druidDataSource.setMinIdle(minIdle);
        }
    }

    public boolean checkPoolIsHealthy(String resourceId) {
        DruidDataSource druidDataSource = druidDataSourceMap.get(resourceId);
        if (druidDataSource == null) {
            return false;
        }
        try (Connection conn = druidDataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                return stmt.execute();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public void logPoolStatus(String resourceId) {
        DruidDataSource druidDataSource = druidDataSourceMap.get(resourceId);

        LOGGER.info("{}-Active Connections: {}", resourceId, druidDataSource.getActiveCount());
        LOGGER.info("{}-Idle Connections: {}", resourceId, druidDataSource.getPoolingCount());
        if (druidDataSource.getActiveCount() > druidDataSource.getMaxActive() * 0.8) {
            LOGGER.warn("{}-Warning: Active connections exceed 80% of max allowed connections!", resourceId);
        }
    }

}
