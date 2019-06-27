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
package io.seata.metrics.exporter.prometheus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.exporter.HTTPServer;
import io.seata.config.ConfigurationFactory;
import io.seata.metrics.Exporter;
import io.seata.metrics.Measurement;
import io.seata.metrics.Registry;
import io.seata.metrics.constants.ConfigurationKeys;

/**
 * Exporter for Prometheus
 *
 * @author zhengyangyong
 */
public class PrometheusExporter extends Collector implements Collector.Describable, Exporter {
    private final HTTPServer server;

    private Registry registry;

    public PrometheusExporter() throws IOException {
        int port = ConfigurationFactory.getInstance().getInt(
            ConfigurationKeys.METRICS_PREFIX + "exporter.prometheus.port", 9898);
        this.server = new HTTPServer(port, true);
        this.register();
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

            if (samples.size() != 0) {
                familySamples.add(new MetricFamilySamples("seata", Type.UNTYPED, "seata", samples));
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

    @Override
    public List<MetricFamilySamples> describe() {
        return collect();
    }

    @Override
    public void close() {
        server.stop();
    }

}