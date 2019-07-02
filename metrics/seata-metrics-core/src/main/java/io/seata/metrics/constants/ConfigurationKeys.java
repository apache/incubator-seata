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
package io.seata.metrics.constants;

/**
 * ConfigurationKeys for metrics config,these key are not in io.seata.core.constants.ConfigurationKeys because metrics is an optional feature
 *
 * @author zhengyangyong
 */
public class ConfigurationKeys {
    public static final String METRICS_PREFIX = "metrics.";

    public static final String METRICS_REGISTRY_TYPE = "registry-type";

    public static final String METRICS_EXPORTER_LIST = "exporter-list";

    public static final String METRICS_EXPORTER_PROMETHEUS_PORT = "exporter-prometheus-port";
}
