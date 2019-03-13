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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class DefaultRegistry implements Registry {
  private static final Map<UUID, Meter> meters;

  static {
    meters = new ConcurrentHashMap<>();
  }

  @Override
  public <T extends Number> Gauge<T> getGauge(Id id, Supplier<T> supplier) {
    return (Gauge<T>) DefaultRegistry.meters.computeIfAbsent(id.getId(), key -> new DefaultGauge<>(id, supplier));
  }

  @Override
  public Counter getCounter(Id id) {
    return (Counter) DefaultRegistry.meters.computeIfAbsent(id.getId(), key -> new DefaultCounter(id));
  }

  @Override
  public Summary getSummary(Id id) {
    return (Summary) DefaultRegistry.meters.computeIfAbsent(id.getId(), key -> new DefaultSummary(id));
  }

  @Override
  public Timer getTimer(Id id) {
    return (Timer) DefaultRegistry.meters.computeIfAbsent(id.getId(), key -> new DefaultTimer(id));
  }

  @Override
  public Iterable<Measurement> measure() {
    List<Measurement> measurements = new ArrayList<>();
    if (DefaultRegistry.meters.size() == 0) {
      return measurements;
    }
    DefaultRegistry.meters.values().iterator().forEachRemaining(meter -> meter.measure().forEach(measurements::add));
    return measurements;
  }
}
