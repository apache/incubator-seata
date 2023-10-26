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

import java.util.List;

public class MetaResponse {
    private List<Cluster> clusterList;
    private long term;

    public MetaResponse() {
    }

    public MetaResponse(List<Cluster> clusterList, long term) {
        this.clusterList = clusterList;
        this.term = term;
    }

    public List<Cluster> getClusterList() {
        return clusterList;
    }

    public void setClusterList(List<Cluster> clusterList) {
        this.clusterList = clusterList;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }
}
