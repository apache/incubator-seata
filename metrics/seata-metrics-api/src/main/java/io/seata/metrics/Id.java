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

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Meter id
 *
 * @author zhengyangyong
 */
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

    public Id(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.tags = new TreeMap<>();
    }

    public Id withTag(String name, String value) {
        this.tags.put(name, value);
        return this;
    }

    public Id withTag(Iterable<Entry<String, String>> tags) {
        if (tags != null) {
            for (Entry<String, String> tag : tags) {
                this.tags.put(tag.getKey(), tag.getValue());
            }
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(name);
        builder.append("(");
        if (tags.size() == 0) {
            builder.append(")");
            return builder.toString();
        }
        for (Entry<String, String> tag : tags.entrySet()) {
            builder.append(String.format("%s=%s,", tag.getKey(), tag.getValue()));
        }
        builder.delete(builder.length() - 1, builder.length());
        builder.append(")");
        return builder.toString();
    }
}
