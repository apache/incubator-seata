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

import org.apache.seata.common.metadata.Node;

import java.util.Objects;


public class NamingServerNode extends Node {
    private double weight = 1.0;
    private boolean healthy = true;
    private long term;
    private String unit;

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getWeight() {
        return weight;
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

    @Override
    public int hashCode() {
        return Objects.hash(getControl(), getTransaction());
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
        return Objects.equals(getControl(), node.getControl()) && Objects.equals(getTransaction(), node.getTransaction());
    }


    public boolean isChanged(Object obj) {
        if (Objects.isNull(obj)) {
            return false;
        }
        NamingServerNode otherNode = (NamingServerNode) obj;

        // other node is newer than me
        return otherNode.term > term;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
