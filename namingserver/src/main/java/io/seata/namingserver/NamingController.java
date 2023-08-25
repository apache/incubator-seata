package io.seata.namingserver;

import io.seata.discovery.registry.namingserver.ClusterResponse;
import io.seata.discovery.registry.namingserver.NamingInstance;
import io.seata.namingserver.listener.Watcher;
import io.seata.namingserver.manager.ClusterWatcherManager;
import io.seata.namingserver.manager.NamingManager;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/naming/v1")
public class NamingController {
    @Resource
    private NamingManager namingManager;

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    @PostMapping("/register")
    public void registerInstance(@RequestBody NamingInstance registerBody) {
        namingManager.registerInstance(registerBody, registerBody.getNamespace());
    }

    @PostMapping("/unregister")
    public void unregisterInstance(@RequestBody NamingInstance registerBody){
        namingManager.unregisterInstance(registerBody, registerBody.getNamespace());
    }

    @GetMapping("/discovery")
    public ClusterResponse discovery(@RequestParam String vGroup, @RequestParam String namespace) {
        return new ClusterResponse(namingManager.getInstanceListByVgroup(vGroup, namespace),
                clusterWatcherManager.getTermByvGroup(vGroup));
    }

    @GetMapping("/createGroup")
    public void createGroup(@RequestParam String namespace,
                            @RequestParam String vGroup,
                            @RequestParam String clusterName){
        namingManager.addvGroup(namespace,clusterName,vGroup);
    }

    /**
     *
     * @param clientTerm 客户端保存的订阅时间戳
     * @param vGroup 事务分组名称
     * @param timeout 超时时间
     * @param request 客户端HTTP请求
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
