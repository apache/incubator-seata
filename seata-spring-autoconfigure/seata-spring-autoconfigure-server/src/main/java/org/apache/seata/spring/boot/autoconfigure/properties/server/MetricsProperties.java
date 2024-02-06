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
package org.apache.seata.spring.boot.autoconfigure.properties.server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_ENABLED;
import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_EXPORTER_LIST;
import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_REGISTRY_TYPE;
import static org.apache.seata.common.DefaultValues.DEFAULT_PROMETHEUS_PORT;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.METRICS_PREFIX;


@Component
@ConfigurationProperties(prefix = METRICS_PREFIX)
public class MetricsProperties {
    private boolean enabled = DEFAULT_METRICS_ENABLED;
    private String registryType = DEFAULT_METRICS_REGISTRY_TYPE;
    private String exporterList = DEFAULT_METRICS_EXPORTER_LIST;
    private int exporterPrometheusPort = DEFAULT_PROMETHEUS_PORT;


    public Boolean getEnabled() {
        return enabled;
    }

    public MetricsProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getRegistryType() {
        return registryType;
    }

    public MetricsProperties setRegistryType(String registryType) {
        this.registryType = registryType;
        return this;
    }

    public String getExporterList() {
        return exporterList;
    }

    public MetricsProperties setExporterList(String exporterList) {
        this.exporterList = exporterList;
        return this;
    }

    public int getExporterPrometheusPort() {
        return exporterPrometheusPort;
    }

    public MetricsProperties setExporterPrometheusPort(int exporterPrometheusPort) {
        this.exporterPrometheusPort = exporterPrometheusPort;
        return this;
    }
}
