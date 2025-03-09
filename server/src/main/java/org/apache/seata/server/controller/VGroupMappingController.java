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

import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vgroup/v1")
public class VGroupMappingController {

    private VGroupMappingStoreManager vGroupMappingStoreManager;

    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * add vGroup in cluster
     *
     * @param vGroup
     * @param unit
     * @return SingleResult
     */
    @GetMapping("/addVGroup")
    public SingleResult<Void> addVGroup(@RequestParam String vGroup, @RequestParam String unit) {
        MappingDO mappingDO = new MappingDO();
        mappingDO.setNamespace(Instance.getInstance().getNamespace());
        mappingDO.setCluster(Instance.getInstance().getClusterName());
        mappingDO.setUnit(unit);
        mappingDO.setVGroup(vGroup);
        boolean rst = SessionHolder.getRootVGroupMappingManager().addVGroup(mappingDO);
        Instance.getInstance().setTerm(System.currentTimeMillis());
        if (!rst) {
            return SingleResult.failure("add vGroup failed!");
        }
        return SingleResult.success();
    }

    /**
     * remove vGroup in cluster
     *
     * @param vGroup
     * @return SingleResult
     */
    @GetMapping("/removeVGroup")
    public SingleResult<Void> removeVGroup(@RequestParam String vGroup) {
        boolean rst = SessionHolder.getRootVGroupMappingManager().removeVGroup(vGroup);
        Instance.getInstance().setTerm(System.currentTimeMillis());
        if (!rst) {
            return SingleResult.failure("remove vGroup failed!");
        }
        return SingleResult.success();
    }
}
