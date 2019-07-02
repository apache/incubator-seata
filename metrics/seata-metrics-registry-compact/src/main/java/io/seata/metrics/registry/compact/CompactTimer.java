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
package io.seata.metrics.registry.compact;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.seata.metrics.Clock;
import io.seata.metrics.Id;
import io.seata.metrics.Measurement;
import io.seata.metrics.SystemClock;
import io.seata.metrics.Timer;
import io.seata.metrics.IdConstants;

/**
 * Compact Timer implement with TimerValue
 *
 * @author zhengyangyong
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
