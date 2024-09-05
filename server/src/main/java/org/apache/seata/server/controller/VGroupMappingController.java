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

import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.common.result.Result;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

import static org.apache.seata.common.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static org.apache.seata.common.ConfigurationKeys.FILE_ROOT_REGISTRY;
import static org.apache.seata.common.ConfigurationKeys.FILE_ROOT_TYPE;
import static org.apache.seata.common.ConfigurationKeys.NAMING_SERVER;

@RestController
@RequestMapping("/vgroup/v1")
public class VGroupMappingController {

    private VGroupMappingStoreManager vGroupMappingStoreManager;

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @PostConstruct
    private void init() {
        String type =
            ConfigurationFactory.getInstance().getConfig(FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + FILE_ROOT_TYPE);
        if (StringUtils.equals(type, NAMING_SERVER)) {
            vGroupMappingStoreManager = SessionHolder.getRootVGroupMappingManager();
        }
    }

    /**
     * add vGroup in cluster
     *
     * @param vGroup
     * @return
     */
    @GetMapping("/addVGroup")
    public Result<?> addVGroup(@RequestParam String vGroup, @RequestParam String unit) {
        Result<?> result = new Result<>();
        MappingDO mappingDO = new MappingDO();
        mappingDO.setNamespace(Instance.getInstance().getNamespace());
        mappingDO.setCluster(Instance.getInstance().getClusterName());
        mappingDO.setUnit(unit);
        mappingDO.setVGroup(vGroup);
        boolean rst = vGroupMappingStoreManager.addVGroup(mappingDO);
        Instance.getInstance().setTerm(System.currentTimeMillis());
        if (!rst) {
            result.setCode("500");
            result.setMessage("add vGroup failed!");
        }
        return result;
    }

    /**
     * remove vGroup in cluster
     *
     * @param vGroup
     * @return
     */
    @GetMapping("/removeVGroup")
    public Result<?> removeVGroup(@RequestParam String vGroup) {
        Result<?> result = new Result<>();
        boolean rst = vGroupMappingStoreManager.removeVGroup(vGroup);
        Instance.getInstance().setTerm(System.currentTimeMillis());
        if (!rst) {
            result.setCode("500");
            result.setMessage("remove vGroup failed!");
        }
        return result;
    }


}
