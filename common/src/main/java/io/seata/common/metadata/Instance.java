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

import static io.seata.common.util.CollectionUtils.mapToJsonString;


public class Instance {
    private String namespace;
    private String clusterName;
    private String unit;
    private Node.Endpoint controlEndpoint = new Node.Endpoint();
    private Node.Endpoint transactionEndpoint = new Node.Endpoint();
    private double weight = 1.0;
    private boolean healthy = true;
    private long timeStamp;
    private String role = "member";
    private Map<String, Object> metadata = new HashMap<>();


    private Instance() {
    }

    public static Instance getInstance() {
        return SingletonHolder.serverInstance;
    }


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Node.Endpoint getControlEndpoint() {
        return controlEndpoint;
    }

    public void setControlEndpoint(Node.Endpoint controlEndpoint) {
        this.controlEndpoint = controlEndpoint;
    }

    public Node.Endpoint getTransactionEndpoint() {
        return transactionEndpoint;
    }

    public void setTransactionEndpoint(Node.Endpoint transactionEndpoint) {
        this.transactionEndpoint = transactionEndpoint;
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

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
        Instance instance = (Instance) o;
        return Objects.equals(controlEndpoint, instance.controlEndpoint) && Objects.equals(transactionEndpoint, instance.transactionEndpoint);
    }


    // Recursively convert metadata to JSON
    public String toJsonString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"namespace\": \"").append(namespace).append("\", ");
        sb.append("\"clusterName\": \"").append(clusterName).append("\", ");
        sb.append("\"unit\": \"").append(unit).append("\", ");
        sb.append("\"controlEndpoint\": ").append(controlEndpoint.toString()).append(", ");
        sb.append("\"transactionEndpoint\": ").append(transactionEndpoint.toString()).append(", ");
        sb.append("\"weight\": ").append(weight).append(", ");
        sb.append("\"healthy\": ").append(healthy).append(", ");
        sb.append("\"timeStamp\": ").append(timeStamp).append(", ");
        sb.append("\"metadata\": ");

        // handle metadata kv map
        sb.append(mapToJsonString(metadata));

        sb.append("}");
        return sb.toString();
    }

    private static class SingletonHolder {
        private static final Instance serverInstance = new Instance();
    }


}

