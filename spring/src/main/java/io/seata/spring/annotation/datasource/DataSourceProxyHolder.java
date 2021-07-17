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
import java.util.HashMap;
import java.util.Map;

import io.seata.rm.datasource.SeataDataSourceProxy;

/**
 * the type data source proxy holder
 *
 * @author xingfudeshi@gmail.com
 * @author selfishlover
 */
public class DataSourceProxyHolder {

    private static final Map<DataSource, SeataDataSourceProxy> PROXY_MAP = new HashMap<>(4);

    static SeataDataSourceProxy put(DataSource origin, SeataDataSourceProxy proxy) {
        return PROXY_MAP.put(origin, proxy);
    }

    static SeataDataSourceProxy get(DataSource origin) {
        return PROXY_MAP.get(origin);
    }
}
