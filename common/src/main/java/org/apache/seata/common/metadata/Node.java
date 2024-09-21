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
package org.apache.seata.common.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class Node {

    private Endpoint control;

    private Endpoint transaction;

    private Endpoint internal;

    private double weight = 1.0;
    private boolean healthy = true;
    private long timeStamp;

    private String group;
    private ClusterRole role = ClusterRole.MEMBER;

    private String version;

    private Map<String, Object> metadata = new HashMap<>();

    public Node() {}

    public Endpoint createEndpoint(String host, int port, String protocol) {
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

    public Endpoint getControl() {
        return control;
    }

    public void setControl(Endpoint control) {
        this.control = control;
    }

    public Endpoint getTransaction() {
        return transaction;
    }

    public void setTransaction(Endpoint transaction) {
        this.transaction = transaction;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Endpoint getInternal() {
        return internal;
    }

    public void setInternal(Endpoint internal) {
        this.internal = internal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(control, transaction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node) o;
        return Objects.equals(control, node.control) && Objects.equals(transaction, node.transaction);
    }

    // convert to String
    public String toJsonString(ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Endpoint {

        private String host;
        private String protocol;
        private int port;

        public Endpoint() {}

        public Endpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public Endpoint(String host, int port, String protocol) {
            this.host = host;
            this.port = port;
            this.protocol = protocol;
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
        public int hashCode() {
            return Objects.hash(host,port,protocol);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Endpoint endpoint = (Endpoint) o;
            return Objects.equals(endpoint.host,this.host)
                    && Objects.equals(endpoint.port,this.port)
                    && Objects.equals(endpoint.protocol,this.protocol);
        }

        @Override
        public String toString() {
            return "Endpoint{" + "host='" + host + '\'' + ", port=" + port + '}';
        }
    }

}
