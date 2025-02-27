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
package org.apache.seata.metrics.exporter.prometheus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.exporter.Exporter;
import org.apache.seata.metrics.registry.Registry;

import static org.apache.seata.common.DefaultValues.DEFAULT_PROMETHEUS_PORT;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_EXPORTER_PROMETHEUS_PORT;

/**
 * Exporter for Prometheus
 *
 */
@LoadLevel(name = "prometheus", order = 1)
public class PrometheusExporter extends Collector implements Collector.Describable, Exporter {

    private final HTTPServer server;

    private Registry registry;

    public PrometheusExporter() throws IOException {
        int port = ConfigurationFactory.getInstance().getInt(
            ConfigurationKeys.METRICS_PREFIX + METRICS_EXPORTER_PROMETHEUS_PORT, DEFAULT_PROMETHEUS_PORT);
        CollectorRegistry collectorRegistry = new CollectorRegistry(true);
        this.register(collectorRegistry);
        this.server = new HTTPServer(new InetSocketAddress(port),collectorRegistry, true);
    }

    @Override
    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> familySamples = new ArrayList<>();
        if (registry != null) {
            Iterable<Measurement> measurements = registry.measure();
            List<Sample> samples = new ArrayList<>();
            measurements.forEach(measurement -> samples.add(convertMeasurementToSample(measurement)));

            if (!samples.isEmpty()) {
                Type unknownType = getUnknownType();
                familySamples.add(new MetricFamilySamples("seata", unknownType, "seata", samples));
            }
        }
        return familySamples;
    }

    private Sample convertMeasurementToSample(Measurement measurement) {
        String prometheusName = measurement.getId().getName().replace(".", "_");
        List<String> labelNames = new ArrayList<>();
        List<String> labelValues = new ArrayList<>();
        for (Entry<String, String> tag : measurement.getId().getTags()) {
            labelNames.add(tag.getKey());
            labelValues.add(tag.getValue());
        }
        return new Sample(prometheusName, labelNames, labelValues, measurement.getValue(),
            (long)measurement.getTimestamp());
    }

    /**
     * Compatible with high and low versions of 'io.prometheus:simpleclient'
     *
     * @return the unknown collector type
     */
    public static Type getUnknownType() {
        Type unknownType;
        try {
            unknownType = Type.valueOf("UNKNOWN");
        } catch (IllegalArgumentException e) {
            unknownType = Type.valueOf("UNTYPED");
        }
        return unknownType;
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return collect();
    }

    @Override
    public void close() {
        server.stop();
    }

}
