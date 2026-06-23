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
package org.apache.seata.integration.raft;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class RaftClusterFailoverHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String GROUP_ENV = "SEATA_RAFT_GROUP";
    private static final String WORKSPACE_ENV = "SEATA_RAFT_WORKSPACE";
    private static final String METADATA_ADDRS_ENV = "SEATA_RAFT_METADATA_ADDRS";
    private static final int CONTROL_PORT_BASE = 7091;
    private static final long FAILOVER_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(45);

    private RaftClusterFailoverHelper() {}

    static String forceLeaderReelection(String oldLeaderAddress, String oldLeaderControlAddress, long oldTerm)
            throws Exception {
        killLeader(readLeaderPid(requiredEnv(WORKSPACE_ENV), oldLeaderControlAddress));
        return waitForNewLeader(oldLeaderAddress, oldLeaderControlAddress, oldTerm);
    }

    private static long readLeaderPid(String workspace, String leaderControlAddress) throws IOException {
        int controlPort = Integer.parseInt(leaderControlAddress.substring(leaderControlAddress.lastIndexOf(':') + 1));
        int nodeIndex = controlPort - CONTROL_PORT_BASE + 1;
        Assertions.assertTrue(nodeIndex >= 1 && nodeIndex <= 3, "Unsupported raft control port: " + controlPort);
        File pidFile = new File(workspace, "node-" + nodeIndex + "/pid");
        Assertions.assertTrue(pidFile.isFile(), "Missing raft node pid file: " + pidFile.getAbsolutePath());
        List<String> pidLines = Files.readAllLines(pidFile.toPath(), StandardCharsets.UTF_8);
        Assertions.assertFalse(pidLines.isEmpty(), "Empty raft node pid file: " + pidFile.getAbsolutePath());
        return Long.parseLong(pidLines.get(0).trim());
    }

    private static void killLeader(long pid) throws Exception {
        Process kill = new ProcessBuilder("kill", "-9", String.valueOf(pid)).start();
        int exitCode = kill.waitFor();
        Assertions.assertEquals(0, exitCode, "Failed to kill raft leader pid " + pid);
    }

    private static String waitForNewLeader(String oldLeaderAddress, String oldLeaderControlAddress, long oldTerm)
            throws Exception {
        long deadline = System.currentTimeMillis() + FAILOVER_TIMEOUT_MS;
        MetadataResponse lastResponse = null;
        while (System.currentTimeMillis() < deadline) {
            for (String metadataAddress : requiredEnv(METADATA_ADDRS_ENV).split(",")) {
                MetadataResponse response = fetchMetadata(metadataAddress.trim(), requiredEnv(GROUP_ENV));
                if (response == null) {
                    continue;
                }
                lastResponse = response;
                Node leader = response.getNodes() == null
                        ? null
                        : response.getNodes().stream()
                                .filter(node -> "LEADER".equalsIgnoreCase(String.valueOf(node.getRole())))
                                .findFirst()
                                .orElse(null);
                if (leader == null || response.getTerm() <= oldTerm) {
                    continue;
                }
                String leaderAddress = leader.getTransaction().getHost() + ":"
                        + leader.getTransaction().getPort();
                String leaderControlAddress = leader.getControl().getHost() + ":"
                        + leader.getControl().getPort();
                if (!oldLeaderAddress.equals(leaderAddress) && !oldLeaderControlAddress.equals(leaderControlAddress)) {
                    return leaderAddress;
                }
            }
            Thread.sleep(1000L);
        }
        Assertions.fail("Timed out waiting for raft leader re-election, last metadata: " + lastResponse);
        return oldLeaderAddress;
    }

    private static MetadataResponse fetchMetadata(String metadataAddress, String group) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("http://" + metadataAddress + "/metadata/v1/cluster?group=" + group);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            return OBJECT_MAPPER.readValue(connection.getInputStream(), MetadataResponse.class);
        } catch (IOException ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        Assertions.assertNotNull(value, name + " must be provided");
        Assertions.assertFalse(value.trim().isEmpty(), name + " must not be blank");
        return value.trim();
    }
}
