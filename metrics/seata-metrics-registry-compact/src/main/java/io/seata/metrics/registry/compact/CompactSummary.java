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

import io.seata.metrics.Clock;
import io.seata.metrics.Id;
import io.seata.metrics.Measurement;
import io.seata.metrics.Summary;
import io.seata.metrics.SystemClock;
import io.seata.metrics.IdConstants;

/**
 * Compact Summary implement with SummaryValue
 *
 * @author zhengyangyong
 */
public class CompactSummary implements Summary {
    private final Id id;

    private final Id countId;

    private final Id totalId;

    private final Id tpsId;

    private volatile SummaryValue value;

    private final Clock clock;

    public CompactSummary(Id id) {
        this(id, SystemClock.INSTANCE);
    }

    public CompactSummary(Id id, Clock clock) {
        this.id = id;
        this.countId = new Id(id.getName()).withTag(id.getTags())
            .withTag(IdConstants.STATISTIC_KEY, IdConstants.STATISTIC_VALUE_COUNT);
        this.totalId = new Id(id.getName()).withTag(id.getTags())
            .withTag(IdConstants.STATISTIC_KEY, IdConstants.STATISTIC_VALUE_TOTAL);
        this.tpsId = new Id(id.getName()).withTag(id.getTags())
            .withTag(IdConstants.STATISTIC_KEY, IdConstants.STATISTIC_VALUE_TPS);
        this.value = new SummaryValue(clock.getCurrentMilliseconds());
        this.clock = clock;
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public void increase(long value) {
        this.value.increase(value);
    }

    @Override
    public long total() {
        return this.value.getTotal();
    }

    @Override
    public long count() {
        return this.value.getCount();
    }

    @Override
    public double tps() {
        return this.value.getTps(clock.getCurrentMilliseconds());
    }

    @Override
    public Iterable<Measurement> measure() {
        SummaryValue value = this.value;
        double time = clock.getCurrentMilliseconds();
        this.value = new SummaryValue(time);
        return Arrays.asList(new Measurement(countId, time, value.getCount()),
            new Measurement(totalId, time, value.getTotal()),
            new Measurement(tpsId, time, value.getTps(time)));
    }
}
