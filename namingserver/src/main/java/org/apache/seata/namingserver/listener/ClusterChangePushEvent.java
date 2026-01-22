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

/**
 * Event published when cluster change notifications are pushed to watchers.
 * Used for metrics tracking via Spring's event mechanism.
 */
public class ClusterChangePushEvent extends ApplicationEvent {

    private final String namespace;
    private final String clusterName;
    private final String vgroup;

    public ClusterChangePushEvent(Object source, String namespace, String clusterName, String vgroup) {
        super(source);
        this.namespace = namespace;
        this.clusterName = clusterName;
        this.vgroup = vgroup;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getVgroup() {
        return vgroup;
    }
}
