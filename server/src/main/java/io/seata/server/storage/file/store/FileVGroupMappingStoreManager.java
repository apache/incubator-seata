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
package io.seata.server.storage.file.store;


import io.seata.common.loader.LoadLevel;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.store.MappingDO;
import io.seata.server.store.VGroupMappingStoreManager;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

@LoadLevel(name = "raft")
public class FileVGroupMappingStoreManager implements VGroupMappingStoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVGroupMappingStoreManager.class);

    private final String STORE_KEY = "store";

    private final String RAFT_KEY = "raft";

    private final String FILE_VGROUP_MAPPING_KEY = "file_vgroup_mapping";

    private ReentrantLock writeLock = new ReentrantLock();

    private final String path;

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();


    public FileVGroupMappingStoreManager() {
        String mappingPath = System.getProperty("user.dir") + "mapping.json";
        path = CONFIG.getConfig(STORE_KEY+RAFT_KEY+ FILE_VGROUP_MAPPING_KEY,mappingPath);
    }

    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        HashMap<String, Object> VGroupMapping = load();
        VGroupMapping.put(mappingDO.getVGroup(), mappingDO.getUnit());
        boolean isSaved = save(VGroupMapping);
        if (!isSaved) {
            LOGGER.error("add mapping relationship failed!");
        }
        return isSaved;
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        HashMap<String, Object> VGroupMapping = load();
        VGroupMapping.remove(vGroup);
        boolean isSaved = save(VGroupMapping);
        if (!isSaved) {
            LOGGER.error("remove mapping relationship failed!");
        }
        return isSaved;
    }

    @Override
    public HashMap<String, Object> load() {
        HashMap<String, Object> VGroupMapping = new HashMap<>();
        try {
            File fileToLoad = new File(path);
            if (!fileToLoad.exists()) {
                throw new IOException("File does not exist at path: " + path);
            }

            String fileContent = FileUtils.readFileToString(fileToLoad, "UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            VGroupMapping = objectMapper.readValue(fileContent, new TypeReference<HashMap<String, Object>>() {});

        } catch (Exception e) {
            LOGGER.error("mapping relationship load failed! ", e);
        }
        return VGroupMapping;
    }


    public boolean save(HashMap<String, Object> VGroupMapping) {
        writeLock.lock();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonMapping = objectMapper.writeValueAsString(VGroupMapping);
            FileUtils.writeStringToFile(new File(path), jsonMapping, "UTF-8");
            return true;
        } catch (IOException e) {
            LOGGER.error("mapping relationship saved failed! ", e);
            return false;
        } finally {
            writeLock.unlock();
        }
    }
}
