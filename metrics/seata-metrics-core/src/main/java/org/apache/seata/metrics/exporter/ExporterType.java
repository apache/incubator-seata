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

/**
 * Supported metrics exporter type
 *
 */
public enum ExporterType {
    /**
     * Export metrics data to Prometheus
     */
    PROMETHEUS("prometheus");

    private String name;

    ExporterType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ExporterType getType(String name) {
        if (PROMETHEUS.name().equalsIgnoreCase(name)) {
            return PROMETHEUS;
        } else {
            throw new IllegalArgumentException("not support exporter type: " + name);
        }
    }
}
