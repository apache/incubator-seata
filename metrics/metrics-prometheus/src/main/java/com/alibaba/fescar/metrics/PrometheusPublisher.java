/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fescar.config.ConfigurationFactory;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.exporter.HTTPServer;

public class PrometheusPublisher extends Collector implements Collector.Describable, Publisher {
  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusPublisher.class);

  private final HTTPServer server;

  private final Registry registry;

  public PrometheusPublisher() throws IOException {
    this.registry = new DefaultRegistry();

    int publishPort = ConfigurationFactory.getInstance()
        .getInt(ConfigurationKeys.METRICS_PUBLISHER_PROMETHEUS_PORT, 9898);

    this.server = new HTTPServer(publishPort, true);

    LOGGER.info("Prometheus Publisher Start at Port : " + publishPort);

    this.register();
  }

  @Override
  public List<MetricFamilySamples> collect() {
    List<MetricFamilySamples> familySamples = new ArrayList<>();
    Iterable<Measurement> measurements = registry.measure();
    List<Sample> samples = new ArrayList<>();
    measurements.forEach(measurement -> samples.add(convertMeasurementToSample(measurement)));

    if (samples.size() != 0) {
      familySamples.add(new MetricFamilySamples("fescar", Type.UNTYPED, "fescar", samples));
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
        (long) measurement.getTimestamp());
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