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
import java.util.Objects;


public class Node {

    private Endpoint controlEndpoint = new Endpoint();
    private Endpoint transactionEndpoint = new Endpoint();
    private double weight = 1.0;
    private boolean healthy = true;
    private long timeStamp;
    private String role = "member";


    private Map<String, Object> metadata = new HashMap<>();


    public Node() {
    }

    public Endpoint getControlEndpoint() {
        return controlEndpoint;
    }

    public void setControlEndpoint(Endpoint controlEndpoint) {
        this.controlEndpoint = controlEndpoint;
    }

    public Endpoint getTransactionEndpoint() {
        return transactionEndpoint;
    }

    public void setTransactionEndpoint(Endpoint transactionEndpoint) {
        this.transactionEndpoint = transactionEndpoint;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public long getTimeStamp() {
        return timeStamp;
    }


    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>(4);
        }

        this.metadata.put(key, value);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(controlEndpoint, transactionEndpoint);
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
        return Objects.equals(controlEndpoint, node.controlEndpoint) && Objects.equals(transactionEndpoint, node.transactionEndpoint);
    }

    public boolean isTotalEqual(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Node otherNode = (Node) obj;

        // check each member variable
        return Objects.equals(controlEndpoint, otherNode.controlEndpoint) &&
                Objects.equals(transactionEndpoint, otherNode.transactionEndpoint) &&
                Double.compare(otherNode.weight, weight) == 0 &&
                healthy == otherNode.healthy &&
                Objects.equals(role, otherNode.role) &&
                Objects.equals(metadata, otherNode.metadata);
    }

    // convert to String
    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"controlEndpoint\": ").append(controlEndpoint.toString()).append(", ");
        sb.append("\"transactionEndpoint\": ").append(transactionEndpoint.toString()).append(", ");
        sb.append("\"weight\": ").append(weight).append(", ");
        sb.append("\"healthy\": ").append(healthy).append(", ");
        sb.append("\"timeStamp\": ").append(timeStamp).append(", ");
        sb.append("\"metadata\": {");

        // handle metadata k-v map
        int i = 0;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            i++;
        }

        sb.append("}}");
        return sb.toString();
    }


    public static class Endpoint {

        private String host;

        private int port;

        private String protocol;

        public Endpoint() {
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

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port, protocol);
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
            return Objects.equals(host, endpoint.getHost()) && Objects.equals(port, endpoint.getPort()) && Objects.equals(protocol, endpoint.protocol);
        }

        @Override
        public String toString() {
            return "{" + "\"host\" :\"" + host + "\",\"port\" : " + port + ",\"protocol\" :\"" + protocol + "\"}";
        }
    }

}
