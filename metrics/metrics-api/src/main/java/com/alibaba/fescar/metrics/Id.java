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

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

public class Id {
  private final UUID id;

  private final String name;

  private final SortedMap<String, String> tags;

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Iterable<Entry<String, String>> getTags() {
    return tags.entrySet();
  }

  public int getTagCount() {
    return tags.size();
  }

  public Id(String name, Map<String, String> tags) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.tags = new TreeMap<>();
    if (tags != null) {
      for (Entry<String, String> tag : tags.entrySet()) {
        tags.put(tag.getKey(), tag.getValue());
      }
    }
  }

  public Id withTag(String name, String value) {
    this.tags.put(name, value);
    return this;
  }
}
