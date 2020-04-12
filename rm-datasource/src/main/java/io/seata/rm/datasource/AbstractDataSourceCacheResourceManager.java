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
package io.seata.rm.datasource;

import io.seata.common.executor.Initialize;
import io.seata.core.model.Resource;
import io.seata.rm.AbstractResourceManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract RM with DataSource Cache.
 *
 * @author sharajava
 */
public abstract class AbstractDataSourceCacheResourceManager extends AbstractResourceManager implements Initialize {

    protected Map<String, Resource> dataSourceCache = new ConcurrentHashMap<>();

    /**
     * Instantiates a new Data source manager.
     */
    public AbstractDataSourceCacheResourceManager() {
    }

    @Override
    public abstract void init();

    @Override
    public Map<String, Resource> getManagedResources() {
        return dataSourceCache;
    }

    @Override
    public void registerResource(Resource resource) {
        dataSourceCache.put(resource.getResourceId(), resource);
        super.registerResource(resource);
    }

}
