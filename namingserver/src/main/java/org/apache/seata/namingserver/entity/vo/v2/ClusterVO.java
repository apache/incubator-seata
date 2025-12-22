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
package org.apache.seata.namingserver.entity.vo.v2;

import java.util.ArrayList;
import java.util.List;

/**
 * Value Object representing cluster information for the v2 API.
 * <p>
 * This class encapsulates the information for a single cluster, including
 * its associated vgroups, units, and cluster type.
 */
public class ClusterVO {

    private List<String> vgroups = new ArrayList<>();

    private List<String> units = new ArrayList<>();

    private String type = "default";

    public List<String> getVgroups() {
        return vgroups;
    }

    public void setVgroups(List<String> vgroups) {
        this.vgroups = vgroups;
    }

    public List<String> getUnits() {
        return units;
    }

    public void setUnits(List<String> units) {
        this.units = units;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
