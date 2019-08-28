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
package io.seata.spring.annotation.datasource;

import io.seata.rm.datasource.DataSourceProxy;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * the type data source proxy holder
 *
 * @author xingfudeshi@gmail.com
 * @date 2019/08/23
 */
public class DataSourceProxyHolder {
    private static final int MAP_INITIAL_CAPACITY = 8;
    private ConcurrentHashMap<DataSource, DataSourceProxy> dataSourceProxyMap;

    private DataSourceProxyHolder() {
        dataSourceProxyMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
    }

    /**
     * the type holder
     */
    private static class Holder {
        private static DataSourceProxyHolder INSTANCE;

        static {
            INSTANCE = new DataSourceProxyHolder();
        }

    }

    /**
     * Get DataSourceProxyHolder instance
     *
     * @return the INSTANCE of DataSourceProxyHolder
     */
    public static DataSourceProxyHolder get() {
        return Holder.INSTANCE;
    }

    /**
     * Put dataSource
     *
     * @param dataSource
     * @return dataSourceProxy
     */
    public DataSourceProxy putDataSource(DataSource dataSource) {
        return this.dataSourceProxyMap.computeIfAbsent(dataSource, DataSourceProxy::new);
    }

    /**
     * Get dataSourceProxy
     *
     * @param dataSource
     * @return dataSourceProxy
     */
    public DataSourceProxy getDataSourceProxy(DataSource dataSource) {
        return this.dataSourceProxyMap.get(dataSource);
    }

}
