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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.apache.seata.common.DefaultValues.*;

@Component
public class BusinessDataSourcesProperties implements InitializingBean {

    @Autowired
    private Environment env;

    /**
     * Business database properties instance
     */
    private static final Map<String, DataSourceProperties> datasources = new HashMap<>();

    private static final String BASE_PREFIX = "seata.datasources.";

    @Override
    public void afterPropertiesSet() throws Exception {

        Set<String> dataSourceNames = getDataSourceNames();

        for (String name : dataSourceNames) {
            DataSourceProperties props = new DataSourceProperties();
            String prefix = BASE_PREFIX + name + ".";
            props.setEnabled(env.getProperty(prefix + "enabled", Boolean.class, true));
            props.setDbType(env.getProperty(prefix + "dbType", "mysql"));
            props.setDriverClassName(env.getProperty(prefix + "driverClassName", "com.mysql.cj.jdbc.Driver"));
            props.setUrl(
                    env.getProperty(prefix + "url", "jdbc:mysql://127.0.0.1:3306/seata?rewriteBatchedStatements=true"));
            props.setUsername(env.getProperty(prefix + "username", "mysql"));
            props.setPassword(env.getProperty(prefix + "password", "mysql"));
            props.setDatasource(env.getProperty(prefix + "datasource", "druid"));
            props.setMinConn(env.getProperty(prefix + "minConn", Integer.class, DEFAULT_DB_MIN_CONN));
            props.setMaxConn(env.getProperty(prefix + "maxConn", Integer.class, DEFAULT_DB_MAX_CONN));
            props.setMaxWait(env.getProperty(prefix + "maxWait", Long.class, 5000L));

            String resourceId = getOriginUrl(props.getUrl());

            // Use the database URL as a unique identifier
            datasources.put(resourceId, props);
        }
    }

    /**
     * Extract the base URL without query parameters from the JDBC URL
     */
    private String getOriginUrl(String url) {
        int index = url.indexOf("?");
        if (index != -1) {
            url = url.substring(0, index);
        }
        return url;
    }

    private Set<String> getDataSourceNames() {
        Set<String> names = new HashSet<>();

        // Use the standard PropertySource API to get the data source name
        if (env instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configEnv = (ConfigurableEnvironment) env;

            // Store processed data source names to avoid duplication
            Set<String> processedNames = new HashSet<>();

            for (PropertySource<?> propertySource : configEnv.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    EnumerablePropertySource<?> enumSource = (EnumerablePropertySource<?>) propertySource;

                    for (String propertyName : enumSource.getPropertyNames()) {
                        if (propertyName.startsWith(BASE_PREFIX)) {
                            String[] parts = propertyName.split("\\.");
                            if (parts.length > 3) { // seata.datasources.{name}.{property}
                                String dsName = parts[2];
                                if (!processedNames.contains(dsName)) {
                                    // Confirm that this is a valid data source configuration
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

    public static Set<String> getResourceIds() {
        return datasources.keySet();
    }

    public static class DataSourceProperties {
        private boolean enabled = true;
        private String dbType = "mysql";
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private String url = "jdbc:mysql://127.0.0.1:3306/seata?rewriteBatchedStatements=true";
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
