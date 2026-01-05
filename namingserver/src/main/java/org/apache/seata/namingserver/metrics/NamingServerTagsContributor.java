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
package org.apache.seata.namingserver.metrics;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

/**
 * Custom tags contributor for HTTP server request metrics.
 * Adds Seata business dimensions (namespace, cluster, vgroup) to
 * http.server.requests metrics.
 */
@Component
@ConditionalOnProperty(name = "seata.namingserver.metrics.enabled", havingValue = "true")
public class NamingServerTagsContributor extends DefaultServerRequestObservationConvention {

    private static final String TAG_NAMESPACE = "namespace";
    private static final String TAG_CLUSTER = "cluster";
    private static final String TAG_VGROUP = "vgroup";
    private static final String UNKNOWN = "unknown";

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        KeyValues keyValues = super.getLowCardinalityKeyValues(context);

        // Add namespace tag
        String namespace = context.getCarrier().getParameter(TAG_NAMESPACE);
        keyValues = keyValues.and(KeyValue.of(TAG_NAMESPACE, namespace != null ? namespace : UNKNOWN));

        // Add cluster tag
        String cluster = context.getCarrier().getParameter(TAG_CLUSTER);
        if (cluster == null) {
            cluster = context.getCarrier().getParameter("clusterName");
        }
        keyValues = keyValues.and(KeyValue.of(TAG_CLUSTER, cluster != null ? cluster : UNKNOWN));

        // Add vgroup tag
        String vgroup = context.getCarrier().getParameter(TAG_VGROUP);
        if (vgroup == null) {
            vgroup = context.getCarrier().getParameter("group");
        }
        keyValues = keyValues.and(KeyValue.of(TAG_VGROUP, vgroup != null ? vgroup : UNKNOWN));

        return keyValues;
    }
}
