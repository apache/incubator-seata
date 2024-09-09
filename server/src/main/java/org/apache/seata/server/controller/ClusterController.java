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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.InvokeContext;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.result.Result;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.processor.ConfigProcessor;
import org.apache.seata.config.store.ConfigStoreManager;
import org.apache.seata.config.store.ConfigStoreManagerFactory;
import org.apache.seata.core.serializer.SerializerType;
import org.apache.seata.server.cluster.manager.ClusterConfigWatcherManager;
import org.apache.seata.server.cluster.manager.ClusterWatcherManager;
import org.apache.seata.server.cluster.raft.RaftConfigServer;
import org.apache.seata.server.cluster.raft.RaftConfigServerManager;
import org.apache.seata.server.cluster.raft.RaftServer;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.processor.request.ConfigOperationRequest;
import org.apache.seata.server.cluster.raft.processor.response.ConfigOperationResponse;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.cluster.watch.ConfigWatcher;
import org.apache.seata.server.cluster.watch.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static org.apache.seata.common.ConfigurationKeys.SEATA_FILE_PREFIX_ROOT_CONFIG;
import static org.apache.seata.common.ConfigurationKeys.STORE_MODE;
import static org.apache.seata.common.Constants.RAFT_CONFIG_GROUP;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 */
@RestController
@RequestMapping("/metadata/v1")
public class ClusterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Resource
    private ClusterWatcherManager clusterWatcherManager;

    @Resource
    private ClusterConfigWatcherManager clusterConfigWatcherManager;

    private ServerProperties serverProperties;

    private final ConfigStoreManager configStoreManager = ConfigStoreManagerFactory.getInstance();
    @Resource
    ApplicationContext applicationContext;

    private static final LinkedHashMap<String, String> SUFFIX_MAP = new LinkedHashMap<String, String>(8) {
        {
            put("txt", "properties");
            put("text", "properties");
            put("properties", "properties");
            put("yml", "yaml");
            put("yaml", "yaml");
        }
    };
    @PostConstruct
    private void init() {
        this.serverProperties = applicationContext.getBean(ServerProperties.class);
    }

    @PostMapping("/changeCluster")
    public Result<?> changeCluster(@RequestParam String raftClusterStr) {
        Result<?> result = new Result<>();
        final Configuration newConf = new Configuration();
        if (!newConf.parse(raftClusterStr)) {
            result.setMessage("fail to parse initConf:" + raftClusterStr);
        } else {
            RaftServerManager.groups().forEach(group -> {
                RaftServerManager.getCliServiceInstance().changePeers(group,
                    RouteTable.getInstance().getConfiguration(group), newConf);
                RouteTable.getInstance().updateConfiguration(group, newConf);
            });
        }
        return result;
    }

    @PostMapping("/changeConfigCluster")
    public Result<?> changeConfigCluster(@RequestParam String raftClusterStr) {
        Result<?> result = new Result<>();
        final Configuration newConf = new Configuration();
        if (!newConf.parse(raftClusterStr)) {
            result.setMessage("fail to parse initConf:" + raftClusterStr);
        } else {
            String group = RaftConfigServerManager.getGroup();
            RaftConfigServerManager.getCliServiceInstance().changePeers(group,
                    RouteTable.getInstance().getConfiguration(group), newConf);
            RouteTable.getInstance().updateConfiguration(group, newConf);
        }
        return result;
    }

    @GetMapping("/cluster")
    public MetadataResponse cluster(String group) {
        MetadataResponse metadataResponse = new MetadataResponse();
        if (StringUtils.isBlank(group)) {
            group =
                ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);
        }
        RaftServer raftServer = RaftServerManager.getRaftServer(group);
        if (raftServer != null) {
            String mode = ConfigurationFactory.getInstance().getConfig(STORE_MODE);
            metadataResponse.setStoreMode(mode);
            RouteTable routeTable = RouteTable.getInstance();
            try {
                routeTable.refreshLeader(RaftServerManager.getCliClientServiceInstance(), group, 1000);
                PeerId leader = routeTable.selectLeader(group);
                if (leader != null) {
                    Set<Node> nodes = new HashSet<>();
                    RaftClusterMetadata raftClusterMetadata = raftServer.getRaftStateMachine().getRaftLeaderMetadata();
                    Node leaderNode = raftServer.getRaftStateMachine().getRaftLeaderMetadata().getLeader();
                    leaderNode.setGroup(group);
                    nodes.add(leaderNode);
                    nodes.addAll(raftClusterMetadata.getLearner());
                    nodes.addAll(raftClusterMetadata.getFollowers());
                    metadataResponse.setTerm(raftClusterMetadata.getTerm());
                    metadataResponse.setNodes(new ArrayList<>(nodes));
                }
            } catch (Exception e) {
                LOGGER.error("there is an exception to getting the leader address: {}", e.getMessage(), e);
            }
        }
        return metadataResponse;
    }

    @GetMapping("/config/cluster")
    public MetadataResponse configCluster() {
        MetadataResponse metadataResponse = new MetadataResponse();
        RaftConfigServer raftServer = RaftConfigServerManager.getRaftServer();
        if (raftServer != null) {
            String configType = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_CONFIG
                    + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE);
            metadataResponse.setConfigMode(configType);
            RouteTable routeTable = RouteTable.getInstance();
            try {
                routeTable.refreshLeader(RaftConfigServerManager.getCliClientServiceInstance(), RAFT_CONFIG_GROUP , 1000);
                PeerId leader = routeTable.selectLeader(RAFT_CONFIG_GROUP);
                if (leader != null) {
                    Set<Node> nodes = new HashSet<>();
                    RaftClusterMetadata raftClusterMetadata = raftServer.getRaftStateMachine().getRaftLeaderMetadata();
                    Node leaderNode = raftServer.getRaftStateMachine().getRaftLeaderMetadata().getLeader();
                    leaderNode.setGroup(RAFT_CONFIG_GROUP);
                    nodes.add(leaderNode);
                    nodes.addAll(raftClusterMetadata.getLearner());
                    nodes.addAll(raftClusterMetadata.getFollowers());
                    metadataResponse.setTerm(raftClusterMetadata.getTerm());
                    metadataResponse.setNodes(new ArrayList<>(nodes));
                }
            } catch (Exception e) {
                LOGGER.error("there is an exception to getting the leader address: {}", e.getMessage(), e);
            }
        }
        return metadataResponse;
    }

    @GetMapping("/config/get")
    public ConfigOperationResponse getConfig(String namespace, String dataId, String key) {
        try {
            checkParam(namespace, "namespace");
            checkParam(dataId, "dataId");
            checkParam(key, "key");
        }catch (IllegalArgumentException e){
            return ConfigOperationResponse.fail(e.getMessage());
        }
        ConfigOperationRequest request = ConfigOperationRequest.buildGetRequest(namespace, dataId, key);
        return executeConfigOperationRequest(request);
    }

    @PostMapping("/config/put")
    public ConfigOperationResponse putConfig(String namespace, String dataId, String key, String value) {
        try {
            checkParam(namespace, "namespace");
            checkParam(dataId, "dataId");
            checkParam(key, "key");
            checkParam(value, "value");
        }catch (IllegalArgumentException e){
            return ConfigOperationResponse.fail(e.getMessage());
        }
        ConfigOperationRequest request = ConfigOperationRequest.buildPutRequest(namespace, dataId, key, value);
        return executeConfigOperationRequest(request);
    }

    @DeleteMapping("/config/delete")
    public ConfigOperationResponse deleteConfig(String namespace, String dataId, String key) {
        try {
            checkParam(namespace, "namespace");
            checkParam(dataId, "dataId");
            checkParam(key, "key");
        }catch (IllegalArgumentException e){
            return ConfigOperationResponse.fail(e.getMessage());
        }
        ConfigOperationRequest request = ConfigOperationRequest.buildDeleteRequest(namespace, dataId, key);
        return executeConfigOperationRequest(request);
    }

    @DeleteMapping("/config/deleteAll")
    public ConfigOperationResponse deleteConfig(String namespace, String dataId) {
        try {
            checkParam(namespace, "namespace");
            checkParam(dataId, "dataId");
        }catch (IllegalArgumentException e){
            return ConfigOperationResponse.fail(e.getMessage());
        }
        ConfigOperationRequest request = ConfigOperationRequest.buildDeleteAllRequest(namespace, dataId);
        return executeConfigOperationRequest(request);
    }

    @GetMapping("/config/getAll")
    public ConfigOperationResponse getAllConfig(String namespace, String dataId) {
        try {
            checkParam(namespace, "namespace");
            checkParam(dataId, "dataId");
        }catch (IllegalArgumentException e){
            return ConfigOperationResponse.fail(e.getMessage());
        }
        ConfigOperationRequest request = ConfigOperationRequest.buildGetAllRequest(namespace, dataId);
        return executeConfigOperationRequest(request);
    }

    @PostMapping("/config/upload")
    public ConfigOperationResponse uploadConfig(@RequestParam("namespace") String namespace, @RequestParam("dataId") String dataId, @RequestParam("file") MultipartFile file) {
        try {
            checkParam(namespace, "namespace");
            checkParam(dataId, "dataId");
        }catch (IllegalArgumentException e){
            return ConfigOperationResponse.fail(e.getMessage());
        }
        if (file == null || file.isEmpty()) {
            return ConfigOperationResponse.fail("The configuration file cannot be empty!");
        }
        String fileName = file.getOriginalFilename();
        String dataType = SUFFIX_MAP.get(getFileType(fileName));
        if (StringUtils.isEmpty(dataType)){
            return ConfigOperationResponse.fail("The configuration file type is not supported!");
        }
        StringBuilder sb = new StringBuilder();
        Map<String, Object> configMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            Properties properties = ConfigProcessor.processConfig(sb.toString(), dataType);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                // remove 'seata.' prefix compatible with the config under Spring Boot
                if (key.startsWith(SEATA_FILE_PREFIX_ROOT_CONFIG)) {
                    key = key.substring(SEATA_FILE_PREFIX_ROOT_CONFIG.length());
                }
                configMap.put(key, value);
            }
        }catch (IOException e){
            LOGGER.error("Failed to read config file: {}", e.getMessage());
            return ConfigOperationResponse.fail("Failed to read config file");
        }
        ConfigOperationRequest request = ConfigOperationRequest.buildUploadRequest(namespace, dataId, configMap);
        return executeConfigOperationRequest(request);
    }

    private static String getFileType(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    @GetMapping("/config/getNamespaces")
    public ConfigOperationResponse getNamespaces() {
        ConfigOperationRequest request = ConfigOperationRequest.buildGetNamespaces();
        return executeConfigOperationRequest(request);
    }

    @GetMapping("/config/getDataIds")
    public ConfigOperationResponse getDataIds(String namespace) {
        try {
            checkParam(namespace, "namespace");
        }catch (IllegalArgumentException e){
            return ConfigOperationResponse.fail(e.getMessage());
        }
        ConfigOperationRequest request = ConfigOperationRequest.buildGetDataIds(namespace);
        return executeConfigOperationRequest(request);
    }

    private ConfigOperationResponse executeConfigOperationRequest(ConfigOperationRequest request) {
        PeerId leader = RaftConfigServerManager.getLeader();
        if (leader == null) {
            return ConfigOperationResponse.fail("failed to get leader");
        }
        InvokeContext invokeContext = new InvokeContext();
        invokeContext.put(com.alipay.remoting.InvokeContext.BOLT_CUSTOM_SERIALIZER,
                SerializerType.JACKSON.getCode());
        CliClientServiceImpl cliClientService = (CliClientServiceImpl)RaftConfigServerManager.getCliClientServiceInstance();
        try {
            return (ConfigOperationResponse)cliClientService.getRpcClient().invokeSync(leader.getEndpoint(), request, invokeContext, 1000);
        } catch (Exception e) {
            LOGGER.error("Failed to execute request: {}", request.toString());
            return ConfigOperationResponse.fail(e.getMessage());
        }
    }

    private void checkParam(final String param, final String key) {
        if (StringUtils.isEmpty(param)) {
            throw new IllegalArgumentException("Param '" + key + "' is required.");
        }
    }

    @PostMapping("/watch")
    public void watch(HttpServletRequest request, @RequestParam Map<String, Object> groupTerms,
        @RequestParam(defaultValue = "28000") int timeout) {
        AsyncContext context = request.startAsync();
        context.setTimeout(0L);
        groupTerms.forEach((group, term) -> {
            Watcher<AsyncContext> watcher =
                new Watcher<>(group, context, timeout, Long.parseLong(String.valueOf(term)));
            clusterWatcherManager.registryWatcher(watcher);
        });
    }

    @PostMapping("/config/watch")
    public void watch(HttpServletRequest request, @RequestParam String namespace, @RequestParam String dataId, @RequestParam(required = false) Long version,
                      @RequestParam(defaultValue = "28000") int timeout) {
        Long currentVersion = configStoreManager.getConfigVersion(namespace, dataId);
        // if the config version of client is lower than the server, return directly
        if (version == null || (currentVersion != null && version < currentVersion)) {
            AsyncContext context = request.startAsync();
            HttpServletResponse httpServletResponse = (HttpServletResponse) context.getResponse();
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            context.complete();
            return;
        }
        AsyncContext context = request.startAsync();
        context.setTimeout(0L);
        ConfigWatcher<AsyncContext> configWatcher = new ConfigWatcher<>(namespace, dataId, context, timeout);
        clusterConfigWatcherManager.registryWatcher(configWatcher);
    }

}
