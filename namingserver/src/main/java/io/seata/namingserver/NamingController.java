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
package io.seata.namingserver;

import io.seata.common.metadata.Cluster;
import io.seata.common.metadata.MetaResponse;
import io.seata.common.metadata.Node;
import io.seata.common.metadata.Unit;
import io.seata.core.model.Result;
import io.seata.namingserver.listener.Watcher;
import io.seata.namingserver.manager.ClusterWatcherManager;
import io.seata.namingserver.manager.NamingManager;
import io.seata.namingserver.vo.monitor.ClusterVO;
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
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;

import static io.seata.core.http.HttpServlet.doGet;


@RestController
@RequestMapping("/naming/v1")
public class NamingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingController.class);

    private static final String OK = "200";

    @Resource
    private NamingManager namingManager;

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    @PostMapping("/register")
    public void registerInstance(@RequestParam String namespace,
                                 @RequestParam String clusterName,
                                 @RequestParam String unit,
                                 @RequestBody Node registerBody) {
        namingManager.registerInstance(registerBody, namespace, clusterName, unit);
    }

    @PostMapping("/unregister")
    public void unregisterInstance(@RequestParam String unit,
                                   @RequestBody Node registerBody) {
        namingManager.unregisterInstance(unit, registerBody);
    }

    @GetMapping("/monitor")
    public List<ClusterVO> monitorCluster(String namespace){
        return namingManager.monitorCluster(namespace);
    }

    @GetMapping("/health")
    public String healthCheck() {
        return OK;
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
        namingManager.changeGroup(namespace, clusterName, unitName, vGroup);
        if (clusterList == null || clusterList.size() == 0) {
            return Result.build(200, "no instance in cluster for storing mapping relationship");
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
                            + node.getIp()
                            + ":"
                            + (node.getPort() - 1000)
                            + "/naming/v1/removeVGroup?";
                    HashMap<String, String> params = new HashMap<>();
                    params.put("vGroup", vGroup);
                    params.put("unit", unitName);

                    try (CloseableHttpResponse closeableHttpResponse = doGet(httpUrl, params)) {
                        if (closeableHttpResponse == null || closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                            LOGGER.warn("remove vGroup in old cluster failed");
                        }
                    } catch (IOException e) {
                        LOGGER.error("remove vGroup in new cluster failed");
                    }
                }
            }
        }

        // add vGroup in new cluster
        List<InetSocketAddress> inetSocketAddressList = namingManager.getInstances(namespace, clusterName);
        if (inetSocketAddressList == null || inetSocketAddressList.size() == 0) {
            LOGGER.error("no instance in cluster {}", clusterName);
            return Result.build(301, "no instance in cluster" + clusterName);
        } else {
            InetSocketAddress inetSocketAddress = inetSocketAddressList.get(0);
            String httpUrl = "http://"
                    + inetSocketAddress.getAddress().getHostAddress()
                    + ":"
                    //TODO:
                    + (inetSocketAddress.getPort() - 1000)
                    + "/naming/v1/addVGroup?";
            HashMap<String, String> params = new HashMap<>();
            params.put("vGroup", vGroup);
            params.put("unit", unitName);

            try (CloseableHttpResponse closeableHttpResponse = doGet(httpUrl, params)) {
                if (closeableHttpResponse == null || closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                    return Result.build(500, "add vGroup in new cluster failed");
                }
            } catch (IOException e) {
                LOGGER.error("add vGroup in new cluster failed");
            }

        }




        return Result.ok();
    }

    /**
     * @param clientTerm 客户端保存的订阅时间戳
     * @param vGroup     事务分组名称
     * @param timeout    超时时间
     * @param request    客户端HTTP请求
     */

    @GetMapping("/watch")
    public void watch(@RequestParam String clientTerm,
                      @RequestParam String vGroup,
                      @RequestParam String timeout,
                      HttpServletRequest request) {
        AsyncContext context = request.startAsync();
        context.setTimeout(0L);
        Watcher<AsyncContext> watcher = new Watcher<>(vGroup, context, Integer.parseInt(timeout), Long.parseLong(clientTerm));
        clusterWatcherManager.registryWatcher(watcher);
    }


}
