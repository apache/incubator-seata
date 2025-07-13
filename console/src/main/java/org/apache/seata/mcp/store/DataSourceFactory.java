package org.apache.seata.mcp.store;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.mcp.config.DataSourcesConfiguration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DataSourceFactory {

    private static final Map<String, DataSource> dataSourceMap = new HashMap<String, DataSource>();

    public static DataSource getDataSource(String resourceId) {
        if (dataSourceMap.containsKey(resourceId)) return dataSourceMap.get(resourceId);

        Map<String, DataSourcesConfiguration.DataSourceProperties> datasources =
                DataSourcesConfiguration.getDatasources();
        DataSourcesConfiguration.DataSourceProperties dataSourceProperties = datasources.get(resourceId);

        if (dataSourceProperties == null) {
            throw new StoreException("Cannot find datasource properties:" + resourceId);
        }
        DataSource dataSource = createDataSource(dataSourceProperties, resourceId);
        dataSourceMap.put(resourceId, dataSource);
        return dataSource;
    }

    public static DataSource createDataSource(
            DataSourcesConfiguration.DataSourceProperties dataSourceProperties, String resourceId) {
        if (dataSourceProperties == null) {
            throw new StoreException("Cannot find datasource properties:" + dataSourceProperties);
        }

        String type = dataSourceProperties.getDatasource();
        switch (type) {
            case "druid":
                DruidDataSourceProvider druidDataSourceProvider = new DruidDataSourceProvider();
                return druidDataSourceProvider.generateByResourceId(resourceId);
            case "hikari":
                HikariDataSourceProvider hikariDataSourceProvider = new HikariDataSourceProvider();
                return hikariDataSourceProvider.generateByResourceId(resourceId);
            case "dbcp":
                DbcpDataSourceProvider dbcpDataSourceProvider = new DbcpDataSourceProvider();
                return dbcpDataSourceProvider.generateByResourceId(resourceId);
            default:
                throw new StoreException("Unknown datasource type:" + type);
        }
    }
}
