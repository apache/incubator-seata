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
package org.apache.seata.metrics.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_EXPORTER_LIST;

/**
 * Exporter Factory for load all configured exporters
 *
 */
public class ExporterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExporterFactory.class);

    public static List<Exporter> getInstanceList() {
        List<Exporter> exporters = new ArrayList<>();
        String exporterTypeNameList = ConfigurationFactory.getInstance().getConfig(
            ConfigurationKeys.METRICS_PREFIX + ConfigurationKeys.METRICS_EXPORTER_LIST, DEFAULT_METRICS_EXPORTER_LIST);
        if (!StringUtils.isNullOrEmpty(exporterTypeNameList)) {
            String[] exporterTypeNames = exporterTypeNameList.split(",");
            for (String exporterTypeName : exporterTypeNames) {
                ExporterType exporterType;
                try {
                    exporterType = ExporterType.getType(exporterTypeName);
                    exporters.add(
                        EnhancedServiceLoader.load(Exporter.class, Objects.requireNonNull(exporterType).getName()));
                } catch (Exception exx) {
                    LOGGER.error("not support metrics exporter type: {}",exporterTypeName, exx);
                }
            }
        }
        return exporters;
    }
}
