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
package org.apache.seata.mcp.core.props;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.core.secret.SecretResolver;
import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.seata.common.DefaultValues.DEFAULT_DB_MAX_CONN;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_MIN_CONN;

@Component
public class BusinessDataSourcesProperties implements InitializingBean {

    private final Environment env;

    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    private final SecretResolver secretResolver;

    private final int maxDynamicDataSources;

    private final boolean dynamicRegistrationEnabled;

    private final Set<String> allowedHosts;

    private static final Map<String, DataSourceProperties> datasources = new ConcurrentHashMap<>();

    private static final Map<String, String> dataSourcesNamesAndResourceIds = new ConcurrentHashMap<>();

    private static final Set<String> dynamicResourceIds = ConcurrentHashMap.newKeySet();

    private static final String BASE_PREFIX = "seata.businessDataSources.";

    private static final String MYSQL_DB_TYPE = "mysql";

    private static final String MYSQL_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    private static final String RESOURCE_ID_PREFIX = "business-ds://";

    private static final int DEFAULT_MAX_DYNAMIC_DATA_SOURCES = 100;

    private static final Pattern DATASOURCE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");

    private static final Set<String> SYSTEM_DATABASES =
            new HashSet<>(Arrays.asList("information_schema", "mysql", "performance_schema", "sys"));

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessDataSourcesProperties.class);

    public BusinessDataSourcesProperties(Environment env, ObjectMapper objectMapper, SecretResolver secretResolver) {
        this.env = env;
        this.objectMapper = objectMapper;
        this.secretResolver = secretResolver;
        this.maxDynamicDataSources = env.getProperty(
                "seata.businessDataSources.max-dynamic-size", Integer.class, DEFAULT_MAX_DYNAMIC_DATA_SOURCES);
        this.dynamicRegistrationEnabled =
                env.getProperty("seata.businessDataSources.dynamic-registration.enabled", Boolean.class, false);
        this.allowedHosts =
                parseAllowedHosts(env.getProperty("seata.businessDataSources.dynamic-registration.allowed-hosts", ""));
    }

    @Override
    public void afterPropertiesSet() {
        Set<String> dataSourceNames = getDataSourceNames();
        for (String name : dataSourceNames) {
            DataSourceProperties props = new DataSourceProperties();
            String prefix = BASE_PREFIX + name + ".";
            props.setName(name);
            props.setResourceId(buildResourceId(name));
            props.setEnabled(env.getProperty(prefix + "enabled", Boolean.class, true));
            props.setDynamic(false);
            props.setDbType(env.getProperty(prefix + "dbType", MYSQL_DB_TYPE));
            props.setDriverClassName(env.getProperty(prefix + "driverClassName", MYSQL_DRIVER_CLASS_NAME));
            props.setUrl(env.getProperty(prefix + "url"));
            props.setUsername(env.getProperty(prefix + "username"));
            props.setPassword(env.getProperty(prefix + "password"));
            props.setPasswordSecretRef(env.getProperty(prefix + "passwordSecretRef"));
            if (!StringUtils.hasText(props.getPassword()) && StringUtils.hasText(props.getPasswordSecretRef())) {
                props.setPassword(secretResolver.resolve(props.getPasswordSecretRef()));
            }
            props.setDatasource(env.getProperty(prefix + "datasource", "druid"));
            props.setMinConn(env.getProperty(prefix + "minConn", Integer.class, DEFAULT_DB_MIN_CONN));
            props.setMaxConn(env.getProperty(prefix + "maxConn", Integer.class, DEFAULT_DB_MAX_CONN));
            props.setMaxWait(env.getProperty(prefix + "maxWait", Long.class, 5000L));
            if (!validateDataSourceProperties(props, name)) {
                continue;
            }
            if (props.enabled) {
                datasources.put(props.getResourceId(), props);
                dataSourcesNamesAndResourceIds.put(name, props.getResourceId());
            }
        }
    }

    public synchronized String registerMysqlDataSource(MysqlDataSourceRegisterRequest request) {
        if (!dynamicRegistrationEnabled) {
            throw new IllegalArgumentException("Dynamic business data source registration is disabled");
        }
        DataSourceProperties props = buildDynamicMysqlProperties(request);
        String name = props.getName();
        String resourceId = props.getResourceId();
        if (dataSourcesNamesAndResourceIds.containsKey(name) || datasources.containsKey(resourceId)) {
            throw new IllegalArgumentException("The data source name has already been registered: " + name);
        }
        if (dynamicResourceIds.size() >= maxDynamicDataSources) {
            throw new IllegalArgumentException(
                    "The number of dynamic business data sources exceeds the limit: " + maxDynamicDataSources);
        }
        datasources.put(resourceId, props);
        dataSourcesNamesAndResourceIds.put(name, resourceId);
        dynamicResourceIds.add(resourceId);
        return resourceId;
    }

    public synchronized DataSourceProperties buildDynamicMysqlProperties(MysqlDataSourceRegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Data source registration request cannot be null");
        }
        DataSourceProperties props = new DataSourceProperties();
        props.setName(request.getName());
        props.setResourceId(buildResourceId(request.getName()));
        props.setEnabled(true);
        props.setDynamic(true);
        props.setDbType(MYSQL_DB_TYPE);
        props.setDriverClassName(MYSQL_DRIVER_CLASS_NAME);
        props.setUrl(request.getUrl());
        props.setUsername(request.getUsername());
        props.setPasswordSecretRef(request.getPasswordSecretRef());
        props.setPassword(resolvePassword(request));
        props.setDatasource(StringUtils.hasText(request.getDatasource()) ? request.getDatasource() : "druid");
        props.setMinConn(request.getMinConn() <= 0 ? DEFAULT_DB_MIN_CONN : request.getMinConn());
        props.setMaxConn(request.getMaxConn() <= 0 ? DEFAULT_DB_MAX_CONN : request.getMaxConn());
        props.setMaxWait(request.getMaxWait() == null ? 5000L : request.getMaxWait());
        validateDynamicMysqlProperties(props);
        if (!validateDataSourceProperties(props, props.getName())) {
            throw new IllegalArgumentException("Business DataSource Properties has failure");
        }
        return props;
    }

    public synchronized String unregisterMysqlDataSource(String name) {
        if (!dynamicRegistrationEnabled) {
            throw new IllegalArgumentException("Dynamic business data source registration is disabled");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("The data source name cannot be empty");
        }
        String resourceId = dataSourcesNamesAndResourceIds.get(name);
        if (!StringUtils.hasText(resourceId) || !dynamicResourceIds.contains(resourceId)) {
            throw new IllegalArgumentException("Dynamic data source is not registered: " + name);
        }
        datasources.remove(resourceId);
        dataSourcesNamesAndResourceIds.remove(name);
        dynamicResourceIds.remove(resourceId);
        return resourceId;
    }

    public List<MysqlDataSourceInfo> getMysqlDataSourceInfos() {
        return dataSourcesNamesAndResourceIds.entrySet().stream()
                .map(entry -> toInfo(entry.getKey(), datasources.get(entry.getValue())))
                .filter(info -> info != null)
                .collect(Collectors.toList());
    }

    public String getDatabaseName(String resourceId) {
        DataSourceProperties props = datasources.get(resourceId);
        if (props == null) {
            throw new IllegalArgumentException("Cannot find datasource properties: " + resourceId);
        }
        return props.getDatabaseName();
    }

    private MysqlDataSourceInfo toInfo(String name, DataSourceProperties props) {
        if (props == null) {
            return null;
        }
        MysqlDataSourceInfo info = new MysqlDataSourceInfo();
        info.setName(name);
        info.setResourceId(props.getResourceId());
        info.setDatabaseName(props.getDatabaseName());
        info.setDatasource(props.getDatasource());
        info.setDynamic(props.isDynamic());
        info.setEnabled(props.isEnabled());
        return info;
    }

    private String resolvePassword(MysqlDataSourceRegisterRequest request) {
        if (StringUtils.hasText(request.getPassword())) {
            return request.getPassword();
        }
        return secretResolver.resolve(request.getPasswordSecretRef());
    }

    private boolean validateDataSourceProperties(DataSourceProperties props, String dataSourceName) {
        if (props == null) {
            LOGGER.error("DataSource configuration cannot be null for: {}", dataSourceName);
            return false;
        }
        if (!StringUtils.hasText(props.getName())) {
            LOGGER.error("DataSource name cannot be empty");
            return false;
        }
        if (!DATASOURCE_NAME_PATTERN.matcher(props.getName()).matches()) {
            LOGGER.error("DataSource name contains unsupported characters");
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
        if (!MYSQL_DB_TYPE.equalsIgnoreCase(props.getDbType())) {
            LOGGER.error("Only MySQL business data source is supported: {}", dataSourceName);
            return false;
        }
        if (!MYSQL_DRIVER_CLASS_NAME.equals(props.getDriverClassName())) {
            LOGGER.error("Only MySQL 8 driver is supported for datasource: {}", dataSourceName);
            return false;
        }
        try {
            props.setDatabaseName(parseMysqlDatabaseName(props.getUrl()));
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid MySQL JDBC URL for datasource: {}", dataSourceName);
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
        return true;
    }

    private void validateDynamicMysqlProperties(DataSourceProperties props) {
        if (!props.getUrl().toLowerCase(Locale.ROOT).startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("Only jdbc:mysql:// URL is supported");
        }
        String host = parseMysqlHost(props.getUrl());
        if (!allowedHosts.isEmpty() && !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("MySQL host is not allowed: " + host);
        }
        props.setDatabaseName(parseMysqlDatabaseName(props.getUrl()));
    }

    private String parseMysqlHost(String url) {
        try {
            URI uri = URI.create(url.substring("jdbc:".length()));
            if (!StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("MySQL host cannot be empty");
            }
            return uri.getHost();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid MySQL JDBC URL");
        }
    }

    private String parseMysqlDatabaseName(String url) {
        if (!StringUtils.hasText(url) || !url.toLowerCase(Locale.ROOT).startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("Only jdbc:mysql:// URL is supported");
        }
        try {
            URI uri = URI.create(url.substring("jdbc:".length()));
            String path = uri.getPath();
            if (!StringUtils.hasText(path) || "/".equals(path)) {
                throw new IllegalArgumentException("MySQL JDBC URL must include a database name");
            }
            String databaseName = path.startsWith("/") ? path.substring(1) : path;
            int slashIndex = databaseName.indexOf('/');
            if (slashIndex >= 0) {
                databaseName = databaseName.substring(0, slashIndex);
            }
            if (!StringUtils.hasText(databaseName)) {
                throw new IllegalArgumentException("MySQL JDBC URL must include a database name");
            }
            String normalized = databaseName.toLowerCase(Locale.ROOT);
            if (SYSTEM_DATABASES.contains(normalized)) {
                throw new IllegalArgumentException("MySQL JDBC URL database is not allowed: " + databaseName);
            }
            return databaseName;
        } catch (IllegalArgumentException e) {
            throw e;
        }
    }

    private Set<String> parseAllowedHosts(String hosts) {
        if (!StringUtils.hasText(hosts)) {
            return Collections.emptySet();
        }
        return Arrays.stream(hosts.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(host -> host.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private String buildResourceId(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("The data source name cannot be empty");
        }
        return RESOURCE_ID_PREFIX + name.trim();
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
                                if (!processedNames.contains(dsName)
                                        && env.containsProperty(BASE_PREFIX + dsName + ".url")) {
                                    names.add(dsName);
                                    processedNames.add(dsName);
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

    public static Set<String> getDynamicResourceIds() {
        return dynamicResourceIds;
    }

    static void clear() {
        datasources.clear();
        dataSourcesNamesAndResourceIds.clear();
        dynamicResourceIds.clear();
    }

    public static class DataSourceProperties {
        private boolean enabled = true;
        private boolean dynamic;
        private String name;
        private String resourceId;
        private String databaseName;
        private String dbType = MYSQL_DB_TYPE;
        private String driverClassName = MYSQL_DRIVER_CLASS_NAME;
        private String url = "";
        private String username = "";
        private String password = "";
        private String passwordSecretRef = "";
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

        public boolean isDynamic() {
            return dynamic;
        }

        public void setDynamic(boolean dynamic) {
            this.dynamic = dynamic;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getResourceId() {
            return resourceId;
        }

        public void setResourceId(String resourceId) {
            this.resourceId = resourceId;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public void setDatabaseName(String databaseName) {
            this.databaseName = databaseName;
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

        public String getPasswordSecretRef() {
            return passwordSecretRef;
        }

        public void setPasswordSecretRef(String passwordSecretRef) {
            this.passwordSecretRef = passwordSecretRef;
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
