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

import java.util.HashMap;
import java.util.Map;


public class Node {

    Map<String, Object> metadata = new HashMap<>();
    private Endpoint control;
    private Endpoint transaction;

    private String group;
    private ClusterRole role = ClusterRole.MEMBER;

    public Node() {}

    public Endpoint createEndpoint(String host, int port, String protocol) {
        return new Endpoint(host, port);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ClusterRole getRole() {
        return role;
    }

    public void setRole(ClusterRole role) {
        this.role = role;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Endpoint getControl() {
        return control;
    }

    public void setControl(Endpoint control) {
        this.control = control;
    }

    public Endpoint getTransaction() {
        return transaction;
    }

    public void setTransaction(Endpoint transaction) {
        this.transaction = transaction;
    }

    public static class Endpoint {

        private String host;
        private String protocol;
        private int port;

        public Endpoint() {}

        public Endpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String createAddress() {
            return host + ":" + port;
        }

        @Override
        public String toString() {
            return "Endpoint{" + "host='" + host + '\'' + ", port=" + port + '}';
        }
    }

}
