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
package io.seata.metrics.exporter;

import io.seata.common.exception.NotSupportYetException;

/**
 * Supported metrics exporter type
 *
 * @author zhengyangyong
 */
public enum ExporterType {
    /**
     * Export metrics data to Prometheus
     */
    Prometheus;

    public static ExporterType getType(String name) {
        if (Prometheus.name().equalsIgnoreCase(name)) {
            return Prometheus;
        } else {
            throw new NotSupportYetException("unsupported type:" + name);
        }
    }
}
