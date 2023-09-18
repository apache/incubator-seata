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

import java.util.HashMap;
import java.util.Map;

/**
 * @author funkye
 */
public class Node {

    Map<String,Object> metadata = new HashMap<>();
    private Endpoint httpEndpoint;
    private Endpoint grpcEndpoint;
    private Endpoint nettyEndpoint;
    
    private String group;
    private ClusterRole role = ClusterRole.MEMBER;

    public Node() {
    }

    public Endpoint createEndpoint(String host, int port) {
        return new Endpoint(host, port);
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Endpoint getHttpEndpoint() {
        return httpEndpoint;
    }

    public void setHttpEndpoint(Endpoint httpEndpoint) {
        this.httpEndpoint = httpEndpoint;
    }

    public Endpoint getGrpcEndpoint() {
        return grpcEndpoint;
    }

    public void setGrpcEndpoint(Endpoint grpcEndpoint) {
        this.grpcEndpoint = grpcEndpoint;
    }

    public Endpoint getNettyEndpoint() {
        return nettyEndpoint;
    }

    public void setNettyEndpoint(Endpoint nettyEndpoint) {
        this.nettyEndpoint = nettyEndpoint;
    }

    public static class Endpoint {

        private String host;

        private int port;

        public Endpoint() {
        }

        public Endpoint(String host, int port) {
            this.host=host;
            this.port=port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String createAddress() {
            return host + ":" + port;
        }

        @Override
        public String toString() {
            return "Endpoint{" + "host='" + host + '\'' + ", port=" + port + '}';
        }
    }

}
