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
package org.apache.seata.common.metadata.namingserver;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.seata.common.util.CollectionUtils.mapToJsonString;


public class Instance {
    private String namespace;
    private String clusterName;
    private String unit;
    private Node.Endpoint control = new Node.Endpoint();
    private Node.Endpoint transaction = new Node.Endpoint();
    private double weight = 1.0;
    private boolean healthy = true;
    private long term;
    private long timestamp;
    private ClusterRole role = ClusterRole.MEMBER;
    private Map<String, Object> metadata = new HashMap<>();

    private Instance() {
    }

    public static Instance getInstance() {
        return SingletonHolder.SERVER_INSTANCE;
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

    public ClusterRole getRole() {
        return role;
    }

    public void setRole(ClusterRole role) {
        this.role = role;
    }

    public Node.Endpoint getControl() {
        return control;
    }

    public void setControl(Node.Endpoint control) {
        this.control = control;
    }

    public Node.Endpoint getTransaction() {
        return transaction;
    }

    public void setTransaction(Node.Endpoint transaction) {
        this.transaction = transaction;
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

    public long getTerm() {
        return term;
    }


    public void setTerm(long term) {
        this.term = term;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
        Instance instance = (Instance) o;
        return Objects.equals(control, instance.control) && Objects.equals(transaction, instance.transaction);
    }


    public String toJsonString(ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    public Map<String, String> toMap() {
        Map<String, String> resultMap = new HashMap<>();


        resultMap.put("namespace", namespace);
        resultMap.put("clusterName", clusterName);
        resultMap.put("unit", unit);
        resultMap.put("control", control.toString());
        resultMap.put("transaction", transaction.toString());
        resultMap.put("weight", String.valueOf(weight));
        resultMap.put("healthy", String.valueOf(healthy));
        resultMap.put("term", String.valueOf(term));
        resultMap.put("timestamp", String.valueOf(timestamp));
        resultMap.put("metadata", mapToJsonString(metadata));

        return resultMap;
    }

    private static class SingletonHolder {
        private static final Instance SERVER_INSTANCE = new Instance();
    }


}

