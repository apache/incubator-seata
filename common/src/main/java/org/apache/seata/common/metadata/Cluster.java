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

import org.apache.seata.common.metadata.namingserver.Unit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Cluster {
    private String clusterName;
    private String clusterType;
    private List<Unit> unitData = new ArrayList<>();


    public Cluster() {
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public List<Unit> getUnitData() {
        return unitData;
    }

    public void setUnitData(List<Unit> unitData) {
        this.unitData = unitData;
    }

    public void appendUnits(Collection<Unit> unitData) {
        this.unitData.addAll(unitData);
    }

    public void appendUnit(Unit unitData) {
        this.unitData.add(unitData);
    }


}


