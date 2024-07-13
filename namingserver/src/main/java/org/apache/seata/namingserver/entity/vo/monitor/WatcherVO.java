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
package org.apache.seata.namingserver.entity.vo.monitor;

import java.util.List;

public class WatcherVO {
    private String vGroup;
    private List<String> watcherIp;

    public WatcherVO() {
    }

    public WatcherVO(String vGroup, List<String> watcherIp) {
        this.vGroup = vGroup;
        this.watcherIp = watcherIp;
    }

    public String getvGroup() {
        return vGroup;
    }

    public void setvGroup(String vGroup) {
        this.vGroup = vGroup;
    }

    public List<String> getWatcherIp() {
        return watcherIp;
    }

    public void setWatcherIp(List<String> watcherIp) {
        this.watcherIp = watcherIp;
    }
}
