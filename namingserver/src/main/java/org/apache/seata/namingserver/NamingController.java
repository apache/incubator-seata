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
package org.apache.seata.namingserver;

import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.common.metadata.Cluster;
import org.apache.seata.common.metadata.MetaResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.Unit;
import org.apache.seata.common.result.Result;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;
import org.apache.seata.namingserver.manager.NamingManager;
import org.apache.seata.namingserver.vo.monitor.ClusterVO;
import org.apache.seata.namingserver.vo.monitor.WatcherVO;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Result<?> registerInstance(@RequestParam String namespace,
                                      @RequestParam String clusterName,
                                      @RequestParam String unit,
                                      @RequestBody Node registerBody) {
        Result result = new Result();
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
    public Result<?> unregisterInstance(@RequestParam String unit,
                                        @RequestBody Node registerBody) {
        Result result = new Result();
        boolean isSuccess = namingManager.unregisterInstance(unit, registerBody);
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

    @GetMapping("/health")
    public Result<?> healthCheck() {
        return new Result<>();
    }

    @GetMapping("/discovery")
    public MetaResponse discovery(@RequestParam String vGroup, @RequestParam String namespace) {
        return new MetaResponse(namingManager.getClusterListByVgroup(vGroup, namespace),
                clusterWatcherManager.getTermByvGroup(vGroup));
    }

    @PostMapping("/changeGroup")
    public Result<?> changeGroup(@RequestParam String namespace,
                                 @RequestParam String clusterName,
                                 @RequestParam String unitName,
                                 @RequestParam String vGroup) {

        List<Cluster> clusterList = namingManager.getClusterListByVgroup(vGroup, namespace);

        // add vGroup in new cluster
        List<Node> nodeList = namingManager.getInstances(namespace, clusterName);
        if (nodeList == null || nodeList.size() == 0) {
            LOGGER.error("no instance in cluster {}", clusterName);
            return new Result<>("301", "no instance in cluster" + clusterName);
        } else {
            Node node = nodeList.get(0);
            String controlHost = node.getControl().getHost();
            int controlPort = node.getControl().getPort();
            String httpUrl = "http://"
                    + controlHost
                    + ":"
                    + controlPort
                    + "/naming/v1/addVGroup?";
            HashMap<String, String> params = new HashMap<>();
            params.put("vGroup", vGroup);
            params.put("unit", unitName);
            Map<String, String> header = new HashMap<>();
            header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

            try (CloseableHttpResponse closeableHttpResponse = HttpClientUtil.doGet(httpUrl, params,header,30000)) {
                if (closeableHttpResponse == null || closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                    return new Result<>("200", "add vGroup in new cluster failed");
                }
            } catch (IOException e) {
                LOGGER.warn("add vGroup in new cluster failed");
            }

        }

        // remove vGroup in old cluster
        for (Cluster cluster : clusterList) {
            if (cluster.getUnitData() != null && cluster.getUnitData().size() > 0) {
                Unit unit = cluster.getUnitData().get(0);
                if (unit != null
                        && unit.getNamingInstanceList() != null
                        && unit.getNamingInstanceList().size() > 0) {
                    Node node = unit.getNamingInstanceList().get(0);
                    String httpUrl = "http://"
                            + node.getControl().getHost()
                            + ":"
                            + node.getControl().getPort()
                            + "/naming/v1/removeVGroup?";
                    HashMap<String, String> params = new HashMap<>();
                    params.put("vGroup", vGroup);
                    params.put("unit", unitName);
                    Map<String, String> header = new HashMap<>();
                    header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());

                    try (CloseableHttpResponse closeableHttpResponse = HttpClientUtil.doGet(httpUrl, params, header, 30000)) {
                        if (closeableHttpResponse == null || closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                            LOGGER.warn("remove vGroup in old cluster failed");
                        }
                    } catch (IOException e) {
                        LOGGER.warn("remove vGroup in new cluster failed");
                    }
                }
            }
        }

        namingManager.changeGroup(namespace, clusterName, unitName, vGroup);

        return new Result<>();
    }

    /**
     * @param clientTerm 客户端保存的订阅时间戳
     * @param vGroup     事务分组名称
     * @param timeout    超时时间
     * @param request    客户端HTTP请求
     */

    @PostMapping("/watch")
    public void watch(@RequestParam String clientTerm,
                      @RequestParam String vGroup,
                      @RequestParam String timeout,
                      @RequestParam String clientAddr,
                      HttpServletRequest request) {
        AsyncContext context = request.startAsync();
        context.setTimeout(0L);
        Watcher<AsyncContext> watcher = new Watcher<>(vGroup, context, Integer.parseInt(timeout), Long.parseLong(clientTerm), clientAddr);
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
