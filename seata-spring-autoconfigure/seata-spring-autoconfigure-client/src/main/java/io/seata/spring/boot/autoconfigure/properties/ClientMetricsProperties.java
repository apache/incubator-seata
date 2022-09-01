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
package io.seata.spring.boot.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.spring.boot.autoconfigure.StarterConstants.*;

/**
 * @author spilledyear@outlook.com
 */
@Component
@ConfigurationProperties(prefix = CLIENT_METRICS_PREFIX)
public class ClientMetricsProperties {
    protected Boolean enabled = true;
    protected String registryType = "compact";
    protected String exporterList = "prometheus";
    protected Integer exporterPrometheusPort = 9899;

    public Boolean getEnabled() {
        return enabled;
    }

    public ClientMetricsProperties setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getRegistryType() {
        return registryType;
    }

    public ClientMetricsProperties setRegistryType(String registryType) {
        this.registryType = registryType;
        return this;
    }

    public String getExporterList() {
        return exporterList;
    }

    public ClientMetricsProperties setExporterList(String exporterList) {
        this.exporterList = exporterList;
        return this;
    }

    public Integer getExporterPrometheusPort() {
        return exporterPrometheusPort;
    }

    public ClientMetricsProperties setExporterPrometheusPort(Integer exporterPrometheusPort) {
        this.exporterPrometheusPort = exporterPrometheusPort;
        return this;
    }

}
