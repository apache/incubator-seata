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

import org.apache.seata.common.metadata.Cluster;

import java.util.List;
import java.util.Objects;

/**
 * Meta response for naming server
 */
public class MetaResponse {
    private List<Cluster> clusterList;
    private long term;

    /**
     * Default constructor
     */
    public MetaResponse() {}

    /**
     * Constructor with cluster list and term
     *
     * @param clusterList the cluster list
     * @param term the term
     */
    public MetaResponse(List<Cluster> clusterList, long term) {
        this.clusterList = clusterList;
        this.term = term;
    }

    /**
     * Get cluster list
     *
     * @return the cluster list
     */
    public List<Cluster> getClusterList() {
        return clusterList;
    }

    /**
     * Set cluster list
     *
     * @param clusterList the cluster list
     */
    public void setClusterList(List<Cluster> clusterList) {
        this.clusterList = clusterList;
    }

    /**
     * Get term
     *
     * @return the term
     */
    public long getTerm() {
        return term;
    }

    /**
     * Set term
     *
     * @param term the term
     */
    public void setTerm(long term) {
        this.term = term;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetaResponse that = (MetaResponse) o;
        return term == that.term && Objects.equals(clusterList, that.clusterList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterList, term);
    }

    @Override
    public String toString() {
        return "MetaResponse{" + "clusterList=" + clusterList + ", term=" + term + '}';
    }
}
