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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Record container for CompactTimer
 *
 * @author zhengyangyong
 */
public class TimerValue {
    private final LongAdder count;

    private final LongAdder total;

    private final AtomicLong max;

    public long getCount() {
        return count.longValue();
    }

    public long getTotal() {
        return total.longValue();
    }

    public long getMax() {
        return max.get();
    }

    public double getAverage() {
        double count = this.count.doubleValue();
        double total = this.total.doubleValue();
        return count == 0 ? 0 : total / count;
    }

    public TimerValue() {
        this.count = new LongAdder();
        this.total = new LongAdder();
        this.max = new AtomicLong(0);
    }

    public void record(long value, TimeUnit unit) {
        if (value < 0) {
            return;
        }
        long changeValue = unit == TimeUnit.MICROSECONDS ? value : TimeUnit.MICROSECONDS.convert(value, unit);
        this.count.increment();
        this.total.add(changeValue);
        this.max.accumulateAndGet(changeValue, Math::max);
    }
}