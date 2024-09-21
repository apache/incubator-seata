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
package org.apache.seata.server.storage.db.store;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.server.store.VGroupMappingStoreManager;

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
    public HashMap<String, Object> loadVGroups() {
        List<MappingDO> mappingDOS = vGroupMappingDataBaseDAO.queryMappingDO();
        Instance instance = Instance.getInstance();
        HashMap<String, Object> mappings = new HashMap<>();
        for (MappingDO mappingDO : mappingDOS) {
            if (mappingDO.getCluster() != null && mappingDO.getCluster().equals(instance.getClusterName())) {
                mappings.put(mappingDO.getVGroup(), null);
            }
        }
        return mappings;
    }


}
