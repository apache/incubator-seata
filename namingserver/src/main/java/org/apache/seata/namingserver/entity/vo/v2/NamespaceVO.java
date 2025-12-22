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

import java.util.HashMap;
import java.util.Map;

/**
 * Value Object representing namespace information for the v2 API.
 * <p>
 * This class provides a mapping between cluster names and their associated cluster information.
 * It is used in the v2 version of the API and may differ from the original {@code NamespaceVO}
 * (in the v1 or root package) in its structure or the semantics of its fields.
 * <p>
 * <b>Differences from the original NamespaceVO:</b>
 * <ul>
 *   <li>Located in the {@code org.apache.seata.namingserver.entity.vo.v2} package, indicating it is for the v2 API.</li>
 *   <li>Contains a {@code clusters} map, which maps cluster names to {@link ClusterVO} objects containing detailed cluster information.</li>
 *   <li>May have a different structure or additional fields compared to the original version.</li>
 * </ul>
 * <p>
 * API consumers should refer to this class when interacting with the v2 endpoints to understand
 * the data structure being returned.
 */
public class NamespaceVO {

    private Map<String, ClusterVO> clusters = new HashMap<>();

    public Map<String, ClusterVO> getClusters() {
        return clusters;
    }

    public void setClusters(Map<String, ClusterVO> clusters) {
        this.clusters = clusters;
    }
}
