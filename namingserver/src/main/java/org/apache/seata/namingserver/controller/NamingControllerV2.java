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
package org.apache.seata.namingserver.controller;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.namingserver.entity.vo.v2.NamespaceVO;
import org.apache.seata.namingserver.manager.NamingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping(value = {"/naming/v2", "/api/v2/naming"})
public class NamingControllerV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamingControllerV2.class);

    @Resource
    private NamingManager namingManager;

    /**
     * Retrieves all namespaces.
     * <p>
     * API Endpoint: GET /naming/v2/namespace or /api/v2/naming/namespace
     * </p>
     *
     * @return a {@link SingleResult} containing a map where the key is the namespace name and the value is a {@link NamespaceVO} object
     */
    @GetMapping("/namespace")
    public SingleResult<Map<String, NamespaceVO>> namespaces() {
        return namingManager.namespaceV2();
    }
}
