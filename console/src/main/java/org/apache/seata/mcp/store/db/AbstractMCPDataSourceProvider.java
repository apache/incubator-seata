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
package org.apache.seata.mcp.store.db;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.constants.DBType;
import org.apache.seata.mcp.entity.pojo.BusinessDataSourcesProperties;

import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.seata.common.DefaultValues.DEFAULT_DB_MAX_CONN;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_MIN_CONN;

public abstract class AbstractMCPDataSourceProvider implements Initialize {

    private final Map<String, DataSource> dataSources = new HashMap<>();

    private String resourceId;

    protected static final Map<String, BusinessDataSourcesProperties.DataSourceProperties> DATASOURCE_PROPERTIES =
            BusinessDataSourcesProperties.getDatasources();

    private static final String MYSQL_DRIVER_CLASS_NAME = "com.mysql.jdbc.Driver";

    private static final String MYSQL8_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    private static final String MYSQL_DRIVER_FILE_PREFIX = "mysql-connector-j";

    private static final Map<String, ClassLoader> DRIVER_LOADERS;

    private static final long DEFAULT_DB_MAX_WAIT = 5000;

    static {
        DRIVER_LOADERS = createMysqlDriverClassLoaders();
    }

    @Override
    public void init() {
        for (String key : DATASOURCE_PROPERTIES.keySet()) {
            DataSource dataSource = generateByResourceId(key);
            dataSources.put(key, dataSource);
        }
    }

    public DataSource generate() {
        return doGenerate();
    }

    public DataSource generateByResourceId(String resourceId) {
        this.resourceId = resourceId;
        return generate();
    }

    public void validate() {
        String driverClassName = getDriverClassName();
        ClassLoader loader = getDriverClassLoader();
        if (null == loader) {
            throw new StoreException("class loader set error, you should not use the Bootstrap classloader");
        }
        try {
            loader.loadClass(driverClassName);
        } catch (ClassNotFoundException exx) {
            String folderPath = System.getProperty("loader.path");
            if (folderPath == null) {
                folderPath = System.getProperty("java.class.path");
            }
            String driverClassPath = Stream.of(folderPath.split(File.pathSeparator))
                    .map(File::new)
                    .filter(File::exists)
                    .map(file -> file.isFile() ? file.getParentFile() : file)
                    .filter(Objects::nonNull)
                    .filter(File::isDirectory)
                    .map(file -> (MYSQL8_DRIVER_CLASS_NAME.equals(driverClassName)
                                    || MYSQL_DRIVER_CLASS_NAME.equals(driverClassName))
                            ? new File(file, "jdbc")
                            : file)
                    .filter(File::exists)
                    .filter(File::isDirectory)
                    .distinct()
                    .findAny()
                    .map(File::getAbsolutePath)
                    .orElseThrow(() -> new ShouldNeverHappenException("cannot find jdbc folder"));
            throw new StoreException(String.format(
                    "The driver {%s} cannot be found in the path %s. Please ensure that the appropriate database driver dependencies are included in the classpath.",
                    driverClassName, driverClassPath));
        }
    }

    public abstract DataSource doGenerate();

    protected DBType getDBType() {
        BusinessDataSourcesProperties.DataSourceProperties properties = getDataSourceProperties();
        if (properties != null) {
            return DBType.valueof(properties.getDbType());
        }
        return null;
    }

    protected BusinessDataSourcesProperties.DataSourceProperties getDataSourceProperties() {
        if (StringUtils.isBlank(resourceId)) {
            if (DATASOURCE_PROPERTIES.size() == 1) {
                return DATASOURCE_PROPERTIES.values().iterator().next();
            }
            throw new StoreException("resourceId is not specified and there are multiple datasource properties");
        }
        return DATASOURCE_PROPERTIES.get(resourceId);
    }

    protected String getDriverClassName() {
        BusinessDataSourcesProperties.DataSourceProperties properties = getDataSourceProperties();
        String driverClassName = "";
        if (properties != null) {
            driverClassName = properties.getDriverClassName();
            if (StringUtils.isBlank(driverClassName)) {
                throw new StoreException(
                        String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME));
            }
        }
        return driverClassName;
    }

    protected Long getMaxWait() {
        BusinessDataSourcesProperties.DataSourceProperties properties = getDataSourceProperties();
        if (properties != null && properties.getMaxWait() != null) {
            return properties.getMaxWait();
        }
        return DEFAULT_DB_MAX_WAIT;
    }

    protected ClassLoader getDriverClassLoader() {
        return DRIVER_LOADERS.getOrDefault(getDriverClassName(), this.getClass().getClassLoader());
    }

    private static Map<String, ClassLoader> createMysqlDriverClassLoaders() {
        Map<String, ClassLoader> loaders = new HashMap<>();
        String cp = System.getProperty("loader.path");
        if (cp == null) {
            cp = System.getProperty("java.class.path");
        }
        if (cp == null || cp.isEmpty()) {
            return loaders;
        }
        Stream.of(cp.split(File.pathSeparator))
                .map(File::new)
                .filter(File::exists)
                .map(file -> file.isFile() ? file.getParentFile() : file)
                .filter(Objects::nonNull)
                .filter(File::isDirectory)
                .map(file -> new File(file, "jdbc"))
                .filter(File::exists)
                .filter(File::isDirectory)
                .distinct()
                .flatMap(file -> {
                    File[] files = file.listFiles((f, name) -> name.startsWith(MYSQL_DRIVER_FILE_PREFIX));
                    if (files != null) {
                        return Stream.of(files);
                    } else {
                        return Stream.of();
                    }
                })
                .forEach(file -> {
                    if (loaders.containsKey(MYSQL8_DRIVER_CLASS_NAME) && loaders.containsKey(MYSQL_DRIVER_CLASS_NAME)) {
                        return;
                    }
                    try {
                        URL url = file.toURI().toURL();
                        ClassLoader loader = new URLClassLoader(new URL[] {url}, ClassLoader.getSystemClassLoader());
                        try {
                            loader.loadClass(MYSQL8_DRIVER_CLASS_NAME);
                            loaders.putIfAbsent(MYSQL8_DRIVER_CLASS_NAME, loader);
                        } catch (ClassNotFoundException e) {
                            loaders.putIfAbsent(MYSQL_DRIVER_CLASS_NAME, loader);
                        }
                    } catch (MalformedURLException ignore) {
                    }
                });
        return loaders;
    }

    protected String getUrl() {
        BusinessDataSourcesProperties.DataSourceProperties properties = getDataSourceProperties();
        String url = "";
        if (properties != null) {
            url = properties.getUrl();
            if (StringUtils.isBlank(url)) {
                throw new StoreException(String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_URL));
            }
        }
        return url;
    }

    protected String getUser() {
        BusinessDataSourcesProperties.DataSourceProperties properties = getDataSourceProperties();
        String username = "";
        if (properties != null) {
            username = properties.getUsername();
            if (StringUtils.isBlank(username)) {
                throw new StoreException(String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_USER));
            }
        }
        return username;
    }

    protected String getPassword() {
        BusinessDataSourcesProperties.DataSourceProperties properties = getDataSourceProperties();
        String password = "";
        if (properties != null) {
            password = properties.getPassword();
            if (StringUtils.isBlank(password)) {
                throw new StoreException(String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_PASSWORD));
            }
        }
        return password;
    }

    protected int getMinConn() {
        BusinessDataSourcesProperties.DataSourceProperties properties = getDataSourceProperties();
        int minConn = -1;
        if (properties != null) {
            minConn = properties.getMinConn();
        }
        return minConn < 0 ? DEFAULT_DB_MIN_CONN : minConn;
    }

    protected int getMaxConn() {
        BusinessDataSourcesProperties.DataSourceProperties properties = getDataSourceProperties();
        int maxConn = -1;
        if (properties != null) {
            maxConn = properties.getMaxConn();
        }
        return maxConn < 0 ? DEFAULT_DB_MAX_CONN : maxConn;
    }

    protected String getValidationQuery(DBType dbType) {
        if (DBType.ORACLE.equals(dbType)) {
            return "select sysdate from dual";
        } else {
            return "select 1";
        }
    }
}
