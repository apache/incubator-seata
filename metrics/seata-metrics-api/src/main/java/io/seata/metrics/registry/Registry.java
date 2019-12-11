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
package io.seata.metrics.registry;

import java.util.function.Supplier;

import io.seata.metrics.Counter;
import io.seata.metrics.Gauge;
import io.seata.metrics.Id;
import io.seata.metrics.Measurement;
import io.seata.metrics.Summary;
import io.seata.metrics.Timer;

/**
 * Registry interface for metrics
 *
 * @author zhengyangyong
 */
public interface Registry {
    <T extends Number> Gauge<T> getGauge(Id id, Supplier<T> supplier);

    Counter getCounter(Id id);

    Summary getSummary(Id id);

    Timer getTimer(Id id);

    Iterable<Measurement> measure();
}
