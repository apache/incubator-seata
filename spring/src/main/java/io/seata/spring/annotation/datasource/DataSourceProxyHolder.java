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

import io.seata.core.model.BranchType;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.SeataDataSource;
import io.seata.rm.datasource.xa.DataSourceProxyXA;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * the type data source proxy holder
 *
 * @author xingfudeshi@gmail.com
 */
public class DataSourceProxyHolder {
    private static final int MAP_INITIAL_CAPACITY = 8;
    private ConcurrentHashMap<DataSource, SeataDataSource> dataSourceProxyMap;

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
     * @param dataSource          the data source
     * @param dataSourceProxyMode the data source proxy mode
     * @return dataSourceProxy
     */
    public DataSource putDataSource(DataSource dataSource, String dataSourceProxyMode) {
        DataSource originalDataSource;
        Function<? super DataSource, ? extends SeataDataSource> mappingFunction;

        if (dataSource instanceof SeataDataSource) {
            SeataDataSource dataSourceProxy = (SeataDataSource) dataSource;
            originalDataSource = dataSourceProxy.getTargetDataSource();
            mappingFunction = ds -> dataSourceProxy;
        } else {
            originalDataSource = dataSource;
            mappingFunction = BranchType.XA.name().equalsIgnoreCase(dataSourceProxyMode)
                    ? DataSourceProxyXA::new : DataSourceProxy::new;
        }

        return this.dataSourceProxyMap.computeIfAbsent(originalDataSource, mappingFunction);
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
