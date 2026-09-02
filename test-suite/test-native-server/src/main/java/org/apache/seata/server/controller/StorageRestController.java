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
package org.apache.seata.server.controller;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.server.dto.StorageRequest;
import org.apache.seata.server.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/storage")
public class StorageRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageRestController.class);

    private final StorageService storageService;

    public StorageRestController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Modify stock quantity
     * @param request request parameters
     * @param keyXid distributed transaction ID
     * @return
     */
    @PostMapping
    public Map<String, Object> storage(
            @RequestBody StorageRequest request,
            @RequestHeader(value = RootContext.KEY_XID, required = false) String keyXid) {
        LOGGER.info("Distributed transaction {}: {}", RootContext.KEY_XID, keyXid);

        storageService.storage(request);
        return Map.of("code", 200);
    }

    @GetMapping("/{commodityCode}")
    public long count(@PathVariable String commodityCode) {
        return storageService.count(commodityCode);
    }
}
