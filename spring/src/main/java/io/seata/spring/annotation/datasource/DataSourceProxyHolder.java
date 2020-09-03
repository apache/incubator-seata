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

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.core.model.BranchType;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.xa.DataSourceProxyXA;

/**
 * the type data source proxy holder
 *
 * @author xingfudeshi@gmail.com
 */
public class DataSourceProxyHolder {
    private static final int MAP_INITIAL_CAPACITY = 8;
    private ConcurrentHashMap<DataSource, DataSource> dataSourceProxyMap;

    private DataSourceProxyHolder() {
        dataSourceProxyMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
    }

    /**
     * the type holder
     */
    private static class Holder {
        private static final DataSourceProxyHolder INSTANCE;

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
     * @param originalDataSource  the original data source
     * @param dataSourceProxyMode the data source proxy mode
     * @return dataSourceProxy
     */
    public DataSource putDataSource(DataSource originalDataSource, String dataSourceProxyMode) {
        return this.dataSourceProxyMap.computeIfAbsent(originalDataSource,
                BranchType.XA.name().equalsIgnoreCase(dataSourceProxyMode) ? DataSourceProxyXA::new : DataSourceProxy::new);
    }

    /**
     * Get dataSourceProxy
     *
     * @param dataSource
     * @return dataSourceProxy
     */
    public DataSource getDataSourceProxy(DataSource dataSource) {
        return this.dataSourceProxyMap.get(dataSource);
    }

}
