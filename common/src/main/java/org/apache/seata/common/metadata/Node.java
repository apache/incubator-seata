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
import org.apache.seata.common.exception.ParseEndpointException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

public class Node {

    private Endpoint control;

    private Endpoint transaction;

    private Endpoint internal;

    protected double weight = 1.0;
    protected boolean healthy = true;
    protected long timeStamp;

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

    private Node.ExternalEndpoint createExternalEndpoint(String host, int controllerPort, int transactionPort) {
        return new Node.ExternalEndpoint(host, controllerPort, transactionPort);
    }

    public List<ExternalEndpoint> createExternalEndpoints(String external) {
        List<Node.ExternalEndpoint> externalEndpoints = new ArrayList<>();
        String[] split = external.split(",");

        for (String s : split) {
            String[] item = s.split(":");
            if (item.length == 3) {
                try {
                    String host = item[0];
                    int controllerPort = Integer.parseInt(item[1]);
                    int transactionPort = Integer.parseInt(item[2]);
                    externalEndpoints.add(createExternalEndpoint(host, controllerPort, transactionPort));
                } catch (NumberFormatException e) {
                    throw new ParseEndpointException("Invalid port number in: " + s);
                }
            } else {
                throw new ParseEndpointException("Invalid format for endpoint: " + s);
            }
        }
        return externalEndpoints;
    }

    public Map<String, Object> updateMetadataWithExternalEndpoints(Map<String, Object> metadata, List<Node.ExternalEndpoint> externalEndpoints) {
        Object obj = metadata.get("external");
        if (obj == null) {
            if (!externalEndpoints.isEmpty()) {
                Map<String, Object> metadataMap = new HashMap<>(metadata);
                metadataMap.put("external", externalEndpoints);
                return metadataMap;
            }
            return metadata;
        }
        if (obj instanceof List) {
            List<Node.ExternalEndpoint> oldList = (List<Node.ExternalEndpoint>) obj;
            oldList.addAll(externalEndpoints);
            return metadata;
        } else {
            throw new ParseEndpointException("Metadata 'external' is not a List.");
        }
    }

    public static class ExternalEndpoint {

        private String host;
        private int controlPort;
        private int transactionPort;

        public ExternalEndpoint(String host, int controlPort, int transactionPort) {
            this.host = host;
            this.controlPort = controlPort;
            this.transactionPort = transactionPort;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getControlPort() {
            return controlPort;
        }

        public void setControlPort(int controlPort) {
            this.controlPort = controlPort;
        }

        public int getTransactionPort() {
            return transactionPort;
        }

        public void setTransactionPort(int transactionPort) {
            this.transactionPort = transactionPort;
        }
    }
}
