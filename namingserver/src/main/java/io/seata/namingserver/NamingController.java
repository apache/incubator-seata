package io.seata.namingserver;

import io.seata.common.metadata.Cluster;
import io.seata.common.metadata.MetaResponse;
import io.seata.common.metadata.Node;
import io.seata.common.metadata.Unit;
import io.seata.core.model.Result;
import io.seata.namingserver.listener.Watcher;
import io.seata.namingserver.manager.ClusterWatcherManager;
import io.seata.namingserver.manager.NamingManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static io.seata.core.http.HttpServlet.doGet;


@RestController
@RequestMapping("/naming/v1")
public class NamingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamingController.class);

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

    @GetMapping("/discovery")
    public MetaResponse discovery(@RequestParam String vGroup, @RequestParam String namespace) {
        return new MetaResponse(namingManager.getClusterListByVgroup(vGroup, namespace),
                clusterWatcherManager.getTermByvGroup(vGroup));
    }

    @GetMapping("/changeGroup")
    public Result<?> changeGroup(@RequestParam String namespace,
                                 @RequestParam String clusterName,
                                 @RequestParam String unitName,
                                 @RequestParam String vGroup) {
        List<Cluster> clusterList = namingManager.getClusterListByVgroup(vGroup, namespace);
        // remove vGroup in old cluster
        for (Cluster cluster : clusterList) {
            if (cluster.getUnitData() != null && cluster.getUnitData().size() > 0) {
                Unit unit = cluster.getUnitData().get(0);
                if (unit.getNamingInstanceList() != null && unit.getNamingInstanceList().size() > 0) {
                    Node node = unit.getNamingInstanceList().get(0);
                    String httpUrl = "http://"
                            + node.getIp()
                            + ":"
                            + (node.getPort() - 1000)
                            + "/naming/v1/removeVGroup?";
                    HashMap<String, String> params = new HashMap<>();
                    params.put("vGroup", vGroup);
                    params.put("unit", unitName);

                    CloseableHttpResponse closeableHttpResponse = doGet(httpUrl, params);
                    if (Objects.requireNonNull(closeableHttpResponse).getStatusLine().getStatusCode() != 200) {
                        LOGGER.warn("remove vGroup in old cluster failed");
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
                    + (inetSocketAddress.getPort() - 1000)
                    + "/naming/v1/addVGroup?";
            HashMap<String, String> params = new HashMap<>();
            params.put("vGroup", vGroup);
            params.put("unit", unitName);
            CloseableHttpResponse closeableHttpResponse = doGet(httpUrl, params);
            if (Objects.requireNonNull(closeableHttpResponse).getStatusLine().getStatusCode() != 200) {
                return Result.build(500, "add vGroup in new cluster failed");
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
