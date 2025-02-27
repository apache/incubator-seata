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
package org.apache.seata.server.storage.file.store;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.apache.commons.io.FileUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LoadLevel(name = "file")
public class FileVGroupMappingStoreManager implements VGroupMappingStoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileVGroupMappingStoreManager.class);

    public static final String ROOT_MAPPING_MANAGER_NAME = "vgroup_mapping.json";

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private String storePath;

    HashMap<String, Object> vGroupMapping = new HashMap<>();


    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    ObjectMapper objectMapper = new ObjectMapper();

    public FileVGroupMappingStoreManager() {
    }

    public FileVGroupMappingStoreManager(String mappingStoreFilePath) {
        storePath = mappingStoreFilePath + File.separator + ROOT_MAPPING_MANAGER_NAME;
    }

    @Override
    public boolean addVGroup(MappingDO mappingDO) {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            vGroupMapping.put(mappingDO.getVGroup(), mappingDO.getUnit());
            boolean isSaved = save(vGroupMapping);

            if (!isSaved) {
                LOGGER.error("add mapping relationship failed!");
            }
            return isSaved;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeVGroup(String vGroup) {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            vGroupMapping.remove(vGroup);
            boolean isSaved = save(vGroupMapping);
            if (!isSaved) {
                LOGGER.error("remove mapping relationship failed!");
            }
            return isSaved;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Map<String, Object> readVGroups() {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            return vGroupMapping;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Map<String, Object> loadVGroups() {
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

            if (!fileContent.isEmpty()) {
                vGroupMapping = objectMapper.readValue(fileContent, new TypeReference<HashMap<String, Object>>() {
                });
            }


        } catch (Exception e) {
            LOGGER.error("mapping relationship load failed! " + e);
        }
        return vGroupMapping;
    }


    public boolean save(HashMap<String, Object> vGroupMapping) {
        try {
            String jsonMapping = objectMapper.writeValueAsString(vGroupMapping);
            FileUtils.writeStringToFile(new File(storePath), jsonMapping, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            LOGGER.error("mapping relationship saved failed! ", e);
            return false;
        }
    }
}
