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
import org.apache.seata.common.metadata.Node;

import java.util.Objects;


public class NamingServerNode extends Node {
    private double weight = 1.0;
    private boolean healthy = true;
    private long term;

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

    public boolean isTotalEqual(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        NamingServerNode otherNode = (NamingServerNode) obj;

        // check each member variable
        return Objects.equals(getControl(), otherNode.getControl()) &&
                Objects.equals(getTransaction(), otherNode.getTransaction()) &&
                Double.compare(otherNode.weight, weight) == 0 &&
                healthy == otherNode.healthy &&
                Objects.equals(getRole(), otherNode.getRole()) &&
                term == otherNode.term &&
                Objects.equals(getMetadata(), otherNode.getMetadata());
    }

    // convert to String
    public String toJsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
