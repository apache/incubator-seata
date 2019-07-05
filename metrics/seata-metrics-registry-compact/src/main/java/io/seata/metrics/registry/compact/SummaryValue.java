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

import java.util.concurrent.atomic.LongAdder;

/**
 * Record container for CompactSummary
 *
 * @author zhengyangyong
 */
public class SummaryValue {
    private final LongAdder count;

    private final LongAdder total;

    private final double startMilliseconds;

    public long getCount() {
        return count.longValue();
    }

    public long getTotal() {
        return total.longValue();
    }

    public double getTps(double currentMilliseconds) {
        if (currentMilliseconds <= startMilliseconds) {
            return 0;
        }
        return total.doubleValue() / (currentMilliseconds - startMilliseconds) * 1000.0;
    }

    public SummaryValue(double startMilliseconds) {
        this.count = new LongAdder();
        this.total = new LongAdder();
        this.startMilliseconds = startMilliseconds;
    }

    public void increase() {
        this.increase(1);
    }

    public void increase(long value) {
        if (value <= 0) {
            return;
        }
        this.count.increment();
        this.total.add(value);
    }
}
