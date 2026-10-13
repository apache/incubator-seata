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
package org.apache.seata.server.service.impl;

import org.apache.seata.server.dao.StorageDAO;
import org.apache.seata.server.dto.StorageRequest;
import org.apache.seata.server.entity.Storage;
import org.apache.seata.server.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageServiceImpl implements StorageService {

    private StorageDAO storageDAO;

    @Autowired
    public void setStorageDAO(StorageDAO storageDAO) {
        this.storageDAO = storageDAO;
    }

    @Override
    public Long count(String commodityCode) {
        Storage storage = storageDAO.findByCommodityCode(commodityCode);
        if (storage == null) {
            return null;
        }
        return storage.getCount();
    }

    @Override
    @Transactional
    public void storage(StorageRequest request) {
        String commodityCode = request.getCommodityCode();
        Long count = request.getCount();
        if (count == null) {
            return;
        }

        if (count == 0) {
            return;
        }
        Storage storage = storageDAO.findByCommodityCode(commodityCode);
        if (storage == null) {
            throw new RuntimeException("Commodity does not exist");
        }
        if (count > 0) {
            storage.setCount(storage.getCount() + count);
        } else {
            if (storage.getCount() + count < 0) {
                throw new RuntimeException("Insufficient stock");
            }
            storage.setCount(storage.getCount() + count);
        }
        storageDAO.save(storage);
    }
}
