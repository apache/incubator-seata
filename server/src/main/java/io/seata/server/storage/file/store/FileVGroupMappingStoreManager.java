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

@LoadLevel(name = "file")
public class FileVGroupMappingStoreManager implements VGroupMappingStoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVGroupMappingStoreManager.class);

    private final ReentrantLock writeLock = new ReentrantLock();

    private String storePath;

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();


    public FileVGroupMappingStoreManager() {
    }

    public FileVGroupMappingStoreManager(String name, String mappingStoreFilePath) {
        storePath = mappingStoreFilePath + File.separator + name;
    }

    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        HashMap<String, Object> vGroupMapping = loadVGroups();
        vGroupMapping.put(mappingDO.getVGroup(), mappingDO.getUnit());
        boolean isSaved = save(vGroupMapping);
        if (!isSaved) {
            LOGGER.error("add mapping relationship failed!");
        }
        return isSaved;
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        HashMap<String, Object> vGroupMapping = loadVGroups();
        vGroupMapping.remove(vGroup);
        boolean isSaved = save(vGroupMapping);
        if (!isSaved) {
            LOGGER.error("remove mapping relationship failed!");
        }
        return isSaved;
    }

    @Override
    public HashMap<String, Object> loadVGroups() {
        HashMap<String, Object> vGroupMapping = new HashMap<>();
        try {
            File fileToLoad = new File(storePath);
            if (!fileToLoad.exists()) {
                try {
                    // create new file to record vgroup mapping relationship
                    boolean fileCreated = fileToLoad.createNewFile();
                    if (fileCreated) {
                        LOGGER.info("New vgroup file created at path: " + storePath);
                    } else {
                        LOGGER.warn("Failed to create a new vgroup file at path: " + storePath);
                    }
                } catch (IOException e) {
                    LOGGER.error("Error while creating a new file: " + e.getMessage());
                }
            }

            String fileContent = FileUtils.readFileToString(fileToLoad, "UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            vGroupMapping = objectMapper.readValue(fileContent, new TypeReference<HashMap<String, Object>>() {
            });

        } catch (Exception e) {
            LOGGER.error("mapping relationship load failed! ", e);
        }
        return vGroupMapping;
    }


    public boolean save(HashMap<String, Object> vGroupMapping) {
        writeLock.lock();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonMapping = objectMapper.writeValueAsString(vGroupMapping);
            FileUtils.writeStringToFile(new File(storePath), jsonMapping, "UTF-8");
            return true;
        } catch (IOException e) {
            LOGGER.error("mapping relationship saved failed! ", e);
            return false;
        } finally {
            writeLock.unlock();
        }
    }
}
