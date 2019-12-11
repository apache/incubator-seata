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
package io.seata.metrics;

/**
 * Value of meter
 *
 * @author zhengyangyong
 */
public class Measurement {
    private final Id id;

    private final double timestamp;

    private final double value;

    public Id getId() {
        return id;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }

    public Measurement(Id id, double timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }
}
