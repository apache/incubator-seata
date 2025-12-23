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
 * This class represents the event data received from the cluster watch API.
 * It is used as a DTO (Data Transfer Object) for deserializing server-sent events.
 *
 * <p>The server sends events in SSE format:
 * <pre>
 * data: {"type":"cluster-update|keepalive|timeout","group":"default","term":123,"timestamp":1234567890}
 * </pre>
 *
 * <p>Note: The event type is included in the JSON data, not in a separate SSE "event:" field.
 * This simplifies parsing and reduces the number of lines to read.
 *
 * @see org.apache.seata.common.util.SeataHttpWatch
 */
public class ClusterWatchEvent {

    /**
     * Event type: "cluster-update", "keepalive", or "timeout"
     */
    private String type;

    private String group;

    private Long term;

    private Long timestamp;

    public ClusterWatchEvent() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Long getTerm() {
        return term;
    }

    public void setTerm(Long term) {
        this.term = term;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ClusterWatchEvent{" +
                "type='" + type + '\'' +
                ", group='" + group + '\'' +
                ", term=" + term +
                ", timestamp=" + timestamp +
                '}';
    }
}

