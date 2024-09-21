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


import org.apache.seata.common.metadata.namingserver.MetaResponse;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.result.Result;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;
import org.apache.seata.namingserver.manager.NamingManager;
import org.apache.seata.namingserver.entity.vo.monitor.ClusterVO;
import org.apache.seata.namingserver.entity.vo.monitor.WatcherVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;


import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/naming/v1")
public class NamingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingController.class);


    @Resource
    private NamingManager namingManager;

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    @PostMapping("/register")
    public Result<String> registerInstance(@RequestParam String namespace,
                                           @RequestParam String clusterName,
                                           @RequestParam String unit,
                                           @RequestBody NamingServerNode registerBody) {
        Result<String> result = new Result<>();
        boolean isSuccess = namingManager.registerInstance(registerBody, namespace, clusterName, unit);
        if (isSuccess) {
            result.setMessage("node has registered successfully!");
        } else {
            result.setCode("500");
            result.setMessage("node registered unsuccessfully!");
        }
        return result;
    }

    @PostMapping("/unregister")
    public Result<String> unregisterInstance(@RequestParam String namespace, @RequestParam String clusterName,
        @RequestParam String unit, @RequestBody NamingServerNode registerBody) {
        Result<String> result = new Result<>();
        boolean isSuccess = namingManager.unregisterInstance(namespace, clusterName, unit, registerBody);
        if (isSuccess) {
            result.setMessage("node has unregistered successfully!");
        } else {
            result.setCode("500");
            result.setMessage("node unregistered unsuccessfully!");
        }
        return result;
    }

    @GetMapping("/clusters")
    public List<ClusterVO> monitorCluster(String namespace) {
        return namingManager.monitorCluster(namespace);
    }

    @GetMapping("/discovery")
    public MetaResponse discovery(@RequestParam String vGroup, @RequestParam String namespace) {
        return new MetaResponse(namingManager.getClusterListByVgroup(vGroup, namespace),
                clusterWatcherManager.getTermByvGroup(vGroup));
    }

    @PostMapping("/addGroup")
    public Result<String> addGroup(@RequestParam String namespace,
                                      @RequestParam String clusterName,
                                      @RequestParam String unitName,
                                      @RequestParam String vGroup) {

        Result<String> addGroupResult = namingManager.createGroup(namespace, vGroup, clusterName, unitName);
        if (!addGroupResult.isSuccess()) {
            return addGroupResult;
        }
        return new Result<>("200", "change vGroup " + vGroup + "to cluster " + clusterName + " successfully!");
    }

    @PostMapping("/changeGroup")
    public Result<String> changeGroup(@RequestParam String namespace,
                                      @RequestParam String clusterName,
                                      @RequestParam String unitName,
                                      @RequestParam String vGroup) {

        Result<String> addGroupResult = namingManager.changeGroup(namespace, vGroup, clusterName, unitName);
        if (!addGroupResult.isSuccess()) {
            return addGroupResult;
        }
        return new Result<>("200", "change vGroup " + vGroup + "to cluster " + clusterName + " successfully!");
    }

    /**
     * @param clientTerm The timestamp of the subscription saved on the client side
     * @param vGroup     The name of the transaction group
     * @param timeout    The timeout duration
     * @param request    The client's HTTP request
     */

    @PostMapping("/watch")
    public void watch(@RequestParam String clientTerm,
                      @RequestParam String vGroup,
                      @RequestParam String timeout,
                      HttpServletRequest request) {
        AsyncContext context = request.startAsync();
        context.setTimeout(0L);
        Watcher<AsyncContext> watcher = new Watcher<>(vGroup, context, Integer.parseInt(timeout), Long.parseLong(clientTerm), request.getRemoteAddr());
        clusterWatcherManager.registryWatcher(watcher);
    }

    @GetMapping("/watchList")
    public List<WatcherVO> getWatchList() {
        List<String> watchVGroupList = clusterWatcherManager.getWatchVGroupList();
        return watchVGroupList.stream()
                .map(vgroup -> new WatcherVO(vgroup, clusterWatcherManager.getWatcherIpList(vgroup)))
                .collect(Collectors.toList());
    }


}
