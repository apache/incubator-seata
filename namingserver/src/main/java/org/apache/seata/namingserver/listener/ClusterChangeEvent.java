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
package org.apache.seata.namingserver.listener;

import org.springframework.context.ApplicationEvent;

import java.time.Clock;

public class ClusterChangeEvent extends ApplicationEvent {

    private String group;

    private String namespace;

    private String clusterName;

    private long term;

    public ClusterChangeEvent(Object source, String group, String namespace, String clusterName, long term) {
        super(source);
        this.group = group;
        this.namespace = namespace;
        this.clusterName = clusterName;
        this.term = term;
    }

    public ClusterChangeEvent(Object source, String group, String namespace, String clusterName) {
        super(source);
        this.group = group;
        this.namespace = namespace;
        this.clusterName = clusterName;
    }

    public ClusterChangeEvent(Object source, Clock clock) {
        super(source, clock);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }
}
