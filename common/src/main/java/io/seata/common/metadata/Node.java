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
package io.seata.common.metadata;

import java.util.Objects;

import static io.seata.common.DefaultValues.SERVICE_OFFSET_SPRING_BOOT;

/**
 * @author funkye
 */
public class Node {

    private String host;

    private int nettyPort;

    private int grpcPort;

    private int httpPort;

    private String group;

    public Node(int nettyPort, int raftPort) {
        this.nettyPort = nettyPort;
        if (this.nettyPort <= 0) {
            this.nettyPort = raftPort - SERVICE_OFFSET_SPRING_BOOT;
        }
        this.httpPort = this.nettyPort - SERVICE_OFFSET_SPRING_BOOT;
    }

    public Node() {
    }

    private ClusterRole role = ClusterRole.MEMBER;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ClusterRole getRole() {
        return role;
    }

    public void setRole(ClusterRole role) {
        this.role = role;
    }

    public int getNettyPort() {
        return nettyPort;
    }

    public void setNettyPort(int nettyPort) {
        this.nettyPort = nettyPort;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node)o;
        return Objects.equals(host, node.host) && Objects.equals(nettyPort, node.nettyPort)
            && Objects.equals(grpcPort, node.grpcPort) && Objects.equals(httpPort, node.httpPort)
            && Objects.equals(group, node.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, nettyPort, grpcPort, httpPort, group);
    }

}
