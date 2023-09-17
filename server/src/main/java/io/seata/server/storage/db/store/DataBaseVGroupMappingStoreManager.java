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

@LoadLevel(name="db")
public class DataBaseVGroupMappingStoreManager implements VGroupMappingStoreManager {
    protected VGroupMappingDataBaseDAO vGroupMappingDataBaseDAO;

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    public DataBaseVGroupMappingStoreManager(){
        String datasourceType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        //init dataSource
        DataSource vGroupMappingDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, datasourceType).provide();
        vGroupMappingDataBaseDAO=new VGroupMappingDataBaseDAO(vGroupMappingDataSource);
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
        List<MappingDO> mappingDOS=vGroupMappingDataBaseDAO.queryMappingDO();
        HashMap<String,Object> mappings=new HashMap<>();
        for(MappingDO mappingDO:mappingDOS){
            mappings.put(mappingDO.getVGroup(),null);
        }
        return mappings;
    }


}
