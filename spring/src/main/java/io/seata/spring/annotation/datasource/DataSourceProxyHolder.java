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

import io.seata.common.util.CollectionUtils;
import io.seata.core.model.BranchType;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.SeataDataSourceProxy;
import io.seata.rm.datasource.xa.DataSourceProxyXA;

/**
 * the type data source proxy holder
 *
 * @author xingfudeshi@gmail.com
 */
public class DataSourceProxyHolder {
    private static final int MAP_INITIAL_CAPACITY = 8;
    private ConcurrentHashMap<DataSource, SeataDataSourceProxy> dataSourceProxyMap;

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
    public SeataDataSourceProxy putDataSource(DataSource dataSource, BranchType dataSourceProxyMode) {
        DataSource originalDataSource;
        if (dataSource instanceof SeataDataSourceProxy) {
            SeataDataSourceProxy dataSourceProxy = (SeataDataSourceProxy) dataSource;

            //If it's an right proxy, return it directly.
            if (dataSourceProxyMode == dataSourceProxy.getBranchType()) {
                return (SeataDataSourceProxy)dataSource;
            }

            //Get the original data source.
            originalDataSource = dataSourceProxy.getTargetDataSource();
        } else {
            originalDataSource = dataSource;
        }
        return CollectionUtils.computeIfAbsent(this.dataSourceProxyMap, originalDataSource,
                BranchType.XA == dataSourceProxyMode ? DataSourceProxyXA::new : DataSourceProxy::new);
    }
}
