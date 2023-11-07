package io.seata.server.health;

import io.seata.common.ConfigurationKeys;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.store.db.DataSourceProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @description:
 * @author: zhangjiawei
 * @date: 2023/11/8
 */
@Component
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
public class DataBaseHealthIndicator implements HealthIndicator {

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @Override
    public Health health() {
        String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        DataSource logStoreDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
        try {
            Connection connection = logStoreDataSource.getConnection();
            connection.setAutoCommit(true);

            boolean isConnectionValid = isConnectionValid(connection);

            return Health.status(isConnectionValid ? Status.UP : Status.DOWN)
                    .withDetail("database", getDatabaseProductName(connection))
                    .withDetail("datasourceType", datasourceType)
                    .build();
        } catch (SQLException e) {
            return Health.down().build();
        }
    }

    /**
     * get the database name
     * @param connection
     * @return
     * @throws SQLException
     */
    private String getDatabaseProductName(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName();
    }

    private boolean isConnectionValid(Connection connection) throws SQLException {
        return connection.isValid(0);
    }
}
