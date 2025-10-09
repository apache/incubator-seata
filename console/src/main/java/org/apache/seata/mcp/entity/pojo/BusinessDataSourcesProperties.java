/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.entity.pojo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.seata.common.DefaultValues.DEFAULT_DB_MAX_CONN;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_MIN_CONN;

@Component
public class BusinessDataSourcesProperties implements InitializingBean {

    @Autowired
    private Environment env;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Map<String, DataSourceProperties> datasources = new ConcurrentHashMap<>();

    private static final Map<String, String> dataSourcesNamesAndResourceIds = new ConcurrentHashMap<>();

    private static final String BASE_PREFIX = "seata.businessDataSources.";

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessDataSourcesProperties.class);

    @Override
    public void afterPropertiesSet() {

        Set<String> dataSourceNames = getDataSourceNames();

        for (String name : dataSourceNames) {
            DataSourceProperties props = new DataSourceProperties();
            String prefix = BASE_PREFIX + name + ".";
            props.setEnabled(env.getProperty(prefix + "enabled", Boolean.class, true));
            props.setDbType(env.getProperty(prefix + "dbType", "mysql"));
            props.setDriverClassName(env.getProperty(prefix + "driverClassName", "com.mysql.cj.jdbc.Driver"));
            props.setUrl(env.getProperty(prefix + "url"));
            props.setUsername(env.getProperty(prefix + "username"));
            props.setPassword(env.getProperty(prefix + "password"));
            props.setDatasource(env.getProperty(prefix + "datasource", "druid"));
            props.setMinConn(env.getProperty(prefix + "minConn", Integer.class, DEFAULT_DB_MIN_CONN));
            if (props.getMinConn() <= 0 || props.getMinConn() > DEFAULT_DB_MIN_CONN) {
                LOGGER.warn("The minimum number of connections for a data source: {} is not compliant", name);
                continue;
            }
            props.setMaxConn(env.getProperty(prefix + "maxConn", Integer.class, DEFAULT_DB_MAX_CONN));
            if (props.getMaxConn() <= 0 || props.getMaxConn() > DEFAULT_DB_MAX_CONN) {
                LOGGER.warn("The maximum number of connections for a data source: {} is not compliant", name);
                continue;
            }
            props.setMaxWait(env.getProperty(prefix + "maxWait", Long.class, 5000L));

            if (!validateDataSourceProperties(props, name)) {
                continue;
            }

            String resourceId = getOriginUrl(props.getUrl());

            if (props.enabled) {
                datasources.put(resourceId, props);
                dataSourcesNamesAndResourceIds.put(name, resourceId);
            }
        }
    }

    private boolean validateDataSourceProperties(DataSourceProperties props, String dataSourceName) {
        if (props == null) {
            LOGGER.error("DataSource configuration cannot be null for: {}", dataSourceName);
            return false;
        }

        if (!StringUtils.hasText(props.getUrl())) {
            LOGGER.error("Database URL cannot be empty for datasource: {}", dataSourceName);
            return false;
        }

        if (!StringUtils.hasText(props.getUsername())) {
            LOGGER.error("Database username cannot be empty for datasource: {}", dataSourceName);
            return false;
        }

        if (!StringUtils.hasText(props.getPassword())) {
            LOGGER.error("Database password cannot be empty for datasource: {}", dataSourceName);
            return false;
        }

        if (!StringUtils.hasText(props.getDriverClassName())) {
            LOGGER.error("Database driver class name cannot be empty for datasource: {}", dataSourceName);
            return false;
        }

        if (!StringUtils.hasText(props.getDbType())) {
            LOGGER.error("Database type cannot be empty for datasource: {}", dataSourceName);
            return false;
        }

        if (props.getMinConn() < 0) {
            LOGGER.error("Minimum connection count cannot be negative for datasource: {}", dataSourceName);
            return false;
        }

        if (props.getMaxConn() <= 0) {
            LOGGER.error("Maximum connection count must be positive for datasource: {}", dataSourceName);
            return false;
        }

        if (props.getMinConn() > props.getMaxConn()) {
            LOGGER.error(
                    "Minimum connection count cannot be greater than maximum connection count for datasource: {}",
                    dataSourceName);
            return false;
        }

        if (props.getMaxWait() != null && props.getMaxWait() < 0) {
            LOGGER.error("Maximum wait time cannot be negative for datasource: {}", dataSourceName);
            return false;
        }

        if (!props.getUrl().toLowerCase().startsWith("jdbc:")) {
            LOGGER.error("Invalid JDBC URL format for datasource: {}. URL should start with 'jdbc:'", dataSourceName);
            return false;
        }
        return true;
    }

    private DataSourceProperties parseDBPropertyFromJson(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isEmpty()) {
            throw new IllegalArgumentException("JSON configuration cannot be null");
        }
        DataSourceProperties props = new DataSourceProperties();

        props.setDbType(jsonNode.has("dbType") ? jsonNode.get("dbType").asText() : "mysql");

        String driverClassName = getDefaultDriverClassName(props.getDbType());
        props.setDriverClassName(driverClassName);

        if (!jsonNode.has("url")) {
            throw new IllegalArgumentException("The database URL cannot be empty");
        }
        props.setUrl(jsonNode.get("url").asText());

        if (!jsonNode.has("username")) {
            throw new IllegalArgumentException("The database username cannot be empty");
        }
        props.setUsername(jsonNode.get("username").asText());

        if (!jsonNode.has("password")) {
            throw new IllegalArgumentException("The database password cannot be empty");
        }
        props.setPassword(jsonNode.get("password").asText());

        props.setDatasource(
                jsonNode.has("datasource") ? jsonNode.get("datasource").asText() : "druid");
        props.setMinConn(jsonNode.has("minConn") ? jsonNode.get("minConn").asInt() : DEFAULT_DB_MIN_CONN);
        props.setMaxConn(jsonNode.has("maxConn") ? jsonNode.get("maxConn").asInt() : DEFAULT_DB_MAX_CONN);
        props.setMaxWait(jsonNode.has("maxWait") ? jsonNode.get("maxWait").asLong() : 5000L);

        return props;
    }

    public void registerDataSourceFromJson(String jsonConfig) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(jsonConfig);
        if (jsonNode == null || jsonNode.isEmpty()) {
            throw new IllegalArgumentException("JSON configuration cannot be null");
        }
        String name = jsonNode.get("dbName").asText();
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("The data source name cannot be empty");
        }

        DataSourceProperties props = parseDBPropertyFromJson(jsonNode);
        if (!validateDataSourceProperties(props, name)) {
            throw new IllegalArgumentException("Business DataSource Properties has failure");
        }
        String resourceId = getOriginUrl(props.getUrl());

        datasources.put(resourceId, props);
        dataSourcesNamesAndResourceIds.put(name, resourceId);
    }

    private static String getDefaultDriverClassName(String dbType) {
        switch (dbType.toLowerCase()) {
            case "postgresql":
                return "org.postgresql.Driver";
            case "oracle":
                return "oracle.jdbc.driver.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "h2":
                return "org.h2.Driver";
            case "mysql":
            default:
                return "com.mysql.cj.jdbc.Driver";
        }
    }

    private String getOriginUrl(String url) {
        int index = url.indexOf("?");
        if (index != -1) {
            url = url.substring(0, index);
        }
        return url;
    }

    private Set<String> getDataSourceNames() {
        Set<String> names = new HashSet<>();

        if (env instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configEnv = (ConfigurableEnvironment) env;

            Set<String> processedNames = new HashSet<>();

            for (PropertySource<?> propertySource : configEnv.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    EnumerablePropertySource<?> enumSource = (EnumerablePropertySource<?>) propertySource;

                    for (String propertyName : enumSource.getPropertyNames()) {
                        if (propertyName.startsWith(BASE_PREFIX)) {
                            String[] parts = propertyName.split("\\.");
                            if (parts.length > 3) {
                                String dsName = parts[2];
                                if (!processedNames.contains(dsName)) {
                                    if (env.containsProperty(BASE_PREFIX + dsName + ".url")) {
                                        names.add(dsName);
                                        processedNames.add(dsName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return names;
    }

    public static Map<String, DataSourceProperties> getDatasources() {
        return datasources;
    }

    public static Map<String, String> getDataSourcesNamesAndResourceIds() {
        return dataSourcesNamesAndResourceIds;
    }

    public static Set<String> getResourceIds() {
        return datasources.keySet();
    }

    public static class DataSourceProperties {
        private boolean enabled = true;
        private String dbType = "mysql";
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private String url = "";
        private String username = "mysql";
        private String password = "mysql";
        private String datasource = "druid";
        private int minConn = DEFAULT_DB_MIN_CONN;
        private int maxConn = DEFAULT_DB_MAX_CONN;
        private Long maxWait = 5000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Long getMaxWait() {
            return maxWait;
        }

        public void setMaxWait(Long maxWait) {
            this.maxWait = maxWait;
        }

        public String getDbType() {
            return dbType;
        }

        public void setDbType(String dbType) {
            this.dbType = dbType;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDatasource() {
            return datasource;
        }

        public void setDatasource(String datasource) {
            this.datasource = datasource;
        }

        public int getMinConn() {
            return minConn;
        }

        public void setMinConn(int minConn) {
            this.minConn = minConn;
        }

        public int getMaxConn() {
            return maxConn;
        }

        public void setMaxConn(int maxConn) {
            this.maxConn = maxConn;
        }
    }
}
