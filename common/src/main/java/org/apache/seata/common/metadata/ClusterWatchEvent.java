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
package org.apache.seata.common.metadata;

/**
 * Cluster watch event data class.
 * Simplified format: only contains group, timestamp, and full metadata.
 *
 * <p>Event format:
 * <pre>
 * {"group":"default","timestamp":1234567890,"metadata":{"nodes":[...],"storeMode":"raft","term":2}}
 * </pre>
 *
 * <p>Client can determine if update is needed by comparing metadata.term with local term.
 * No need for separate event type field since all events contain full metadata.
 *
 * @see org.apache.seata.common.util.SeataHttpWatch
 */
public class ClusterWatchEvent {

    private String group;

    private Long timestamp;

    private MetadataResponse metadata;

    public ClusterWatchEvent() {}

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public MetadataResponse getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataResponse metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        String metadataString = metadata == null ? "null" : "MetadataResponse{term=" + metadata.getTerm() + "}";
        return "ClusterWatchEvent{" + "group='"
                + group + '\'' + ", timestamp="
                + timestamp + ", metadata="
                + metadataString + '}';
    }
}
