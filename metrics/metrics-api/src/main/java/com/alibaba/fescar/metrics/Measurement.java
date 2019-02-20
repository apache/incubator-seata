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

import java.util.Map.Entry;

public class Measurement {
  private final Id id;

  private final long timestamp;

  private final double value;

  public Id getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public double getValue() {
    return value;
  }

  public Measurement(Id id, long timestamp, double value) {
    this.id = id;
    this.timestamp = timestamp;
    this.value = value;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(id.getName());
    builder.append("(");
    if (id.getTagCount() == 0) {
      builder.append(")");
      return builder.toString();
    }
    for (Entry<String, String> tag : id.getTags()) {
      builder.append(String.format("%s=%s,", tag.getKey(), tag.getValue()));
    }
    builder.delete(builder.length() - 1, builder.length());
    builder.append(")");
    return builder.toString();
  }
}
