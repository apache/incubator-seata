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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.prometheus.client.Collector.MetricFamilySamples;

public class TestPrometheusPublisher {
  @Test
  public void test() throws IOException {
    PrometheusPublisher prometheusPublisher = new PrometheusPublisher();

    Registry registry = new DefaultRegistry();

    Counter counter = registry.getCounter(new Id("test_counter"));
    counter.increase(888);

    List<MetricFamilySamples> samples = prometheusPublisher.describe();

    Assert.assertEquals(1, samples.size());
    Assert.assertEquals("fescar", samples.get(0).name);
    Assert.assertEquals(1, samples.get(0).samples.size());
    Assert.assertEquals("test_counter", samples.get(0).samples.get(0).name);
    Assert.assertEquals(888, samples.get(0).samples.get(0).value, 0);
  }
}
