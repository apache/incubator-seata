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
package io.seata.server.storage.db.store;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.MappingDO;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.server.store.VGroupMappingStoreManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;

@LoadLevel(name = "db")
public class DataBaseVGroupMappingStoreManager implements VGroupMappingStoreManager {
    protected VGroupMappingDataBaseDAO vGroupMappingDataBaseDAO;

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    public DataBaseVGroupMappingStoreManager() {
        String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        //init dataSource
        DataSource vGroupMappingDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
        vGroupMappingDataBaseDAO = new VGroupMappingDataBaseDAO(vGroupMappingDataSource);
    }

    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        return vGroupMappingDataBaseDAO.insertMappingDO(mappingDO);
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        return vGroupMappingDataBaseDAO.deleteMappingDOByVGroup(vGroup);
    }

    @Override
    public HashMap<String, Object> load() {
        List<MappingDO> mappingDOS = vGroupMappingDataBaseDAO.queryMappingDO();
        HashMap<String, Object> mappings = new HashMap<>();
        for (MappingDO mappingDO : mappingDOS) {
            mappings.put(mappingDO.getVGroup(), null);
        }
        return mappings;
    }


}
