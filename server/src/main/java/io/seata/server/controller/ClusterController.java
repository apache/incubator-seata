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
package io.seata.server.controller;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.metadata.Instance;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.console.result.Result;
import io.seata.core.store.MappingDO;
import io.seata.server.store.VGroupMappingStoreManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/naming/v1")
public class ClusterController {

    private VGroupMappingStoreManager vGroupMappingStoreManager;

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    @PostConstruct
    private void init() {
        String storeType = CONFIG.getConfig("store.mode", "db");
        vGroupMappingStoreManager = EnhancedServiceLoader.load(VGroupMappingStoreManager.class, storeType);

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
        if (!rst) {
            result.setCode("500");
            result.setMessage("add vGroup failed!");
        }
        // push the newest mapping relationship
        vGroupMappingStoreManager.notifyMapping();
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
        if (!rst) {
            result.setCode("500");
            result.setMessage("remove vGroup failed!");
        }
        // push the newest mapping relationship
        vGroupMappingStoreManager.notifyMapping();
        return result;
    }


}
