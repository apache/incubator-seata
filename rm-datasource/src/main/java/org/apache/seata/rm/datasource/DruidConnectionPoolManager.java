package org.apache.seata.rm.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.seata.core.model.Resource;
import org.apache.seata.rm.datasource.entity.ConnectionPoolMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class DruidConnectionPoolManager extends AbstractConnectionPoolManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DruidConnectionPoolManager.class);

    private final Map<String, DruidDataSource> druidDataSourceMap = new ConcurrentHashMap<>();

    public DruidConnectionPoolManager() {

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

    public ConnectionPoolMetrics getConnectionPoolMetrics(String resourceId) {
        DruidDataSource druidDataSource = druidDataSourceMap.get(resourceId);
        if (druidDataSource == null) {
            return null;
        }

        ConnectionPoolMetrics connectionPoolMetrics = new ConnectionPoolMetrics();
        connectionPoolMetrics.setActiveConnections(druidDataSource.getActiveCount());
        connectionPoolMetrics.setCurrentConnections(druidDataSource.getMaxActive());
        connectionPoolMetrics.setIdleConnections(druidDataSource.getPoolingCount());

        return connectionPoolMetrics;
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
