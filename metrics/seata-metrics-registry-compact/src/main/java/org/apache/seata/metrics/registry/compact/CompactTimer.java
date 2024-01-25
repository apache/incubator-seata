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
package org.apache.seata.metrics.registry.compact;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.seata.metrics.Clock;
import org.apache.seata.metrics.Id;
import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.SystemClock;
import org.apache.seata.metrics.Timer;
import org.apache.seata.metrics.IdConstants;

/**
 * Compact Timer implement with TimerValue
 *
 */
public class CompactTimer implements Timer {
    private final Id id;

    private final Id countId;

    private final Id totalId;

    private final Id maxId;

    private final Id averageId;

    private volatile TimerValue value;

    private final Clock clock;

    public CompactTimer(Id id) {
        this(id, SystemClock.INSTANCE);
    }

    public CompactTimer(Id id, Clock clock) {
        this.id = id;
        this.countId = new Id(id.getName()).withTag(id.getTags())
            .withTag(IdConstants.STATISTIC_KEY, IdConstants.STATISTIC_VALUE_COUNT);
        this.totalId = new Id(id.getName()).withTag(id.getTags())
            .withTag(IdConstants.STATISTIC_KEY, IdConstants.STATISTIC_VALUE_TOTAL);
        this.maxId = new Id(id.getName()).withTag(id.getTags())
            .withTag(IdConstants.STATISTIC_KEY, IdConstants.STATISTIC_VALUE_MAX);
        this.averageId = new Id(id.getName()).withTag(id.getTags())
            .withTag(IdConstants.STATISTIC_KEY, IdConstants.STATISTIC_VALUE_AVERAGE);
        this.value = new TimerValue();
        this.clock = clock;
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public void record(long value, TimeUnit unit) {
        this.value.record(value, unit);
    }

    @Override
    public long count() {
        return this.value.getCount();
    }

    @Override
    public long total() {
        return this.value.getTotal();
    }

    @Override
    public long max() {
        return this.value.getMax();
    }

    @Override
    public double average() {
        return this.value.getAverage();
    }

    @Override
    public Iterable<Measurement> measure() {
        //reset value when measure
        double time = clock.getCurrentMilliseconds();
        TimerValue value = this.value;
        this.value = new TimerValue();
        return Arrays.asList(new Measurement(countId, time, value.getCount()),
            new Measurement(totalId, time, value.getTotal() * 0.001),
            new Measurement(maxId, time, value.getMax() * 0.001),
            new Measurement(averageId, time, value.getAverage() * 0.001));
    }
}
