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
package org.apache.seata.spring.boot.autoconfigure.properties.server.raft;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_RAFT_PREFIX;


@Component
@ConfigurationProperties(prefix = SERVER_RAFT_PREFIX)
public class ServerRaftProperties {

    private String serverAddr;

    private String group;

    private Boolean autoJoin = false;

    private Integer snapshotInterval = 600;

    private Integer applyBatch = 32;

    private Integer maxAppendBufferSize = 256 * 1024;

    private Integer maxReplicatorInflightMsgs = 256;

    private Integer disruptorBufferSize = 16384;

    private Integer electionTimeoutMs = 1000;

    private boolean reporterEnabled = false;

    private Integer reporterInitialDelay = 60;

    private String serialization = "jackson";

    private String compressor = "none";

    private boolean sync = true;

    public String getServerAddr() {
        return serverAddr;
    }

    public ServerRaftProperties setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
        return this;
    }

    public Integer getSnapshotInterval() {
        return snapshotInterval;
    }

    public ServerRaftProperties setSnapshotInterval(Integer snapshotInterval) {
        this.snapshotInterval = snapshotInterval;
        return this;
    }

    public Integer getApplyBatch() {
        return applyBatch;
    }

    public ServerRaftProperties setApplyBatch(Integer applyBatch) {
        this.applyBatch = applyBatch;
        return this;
    }

    public Integer getMaxAppendBufferSize() {
        return maxAppendBufferSize;
    }

    public ServerRaftProperties setMaxAppendBufferSize(Integer maxAppendBufferSize) {
        this.maxAppendBufferSize = maxAppendBufferSize;
        return this;
    }

    public Integer getMaxReplicatorInflightMsgs() {
        return maxReplicatorInflightMsgs;
    }

    public ServerRaftProperties setMaxReplicatorInflightMsgs(Integer maxReplicatorInflightMsgs) {
        this.maxReplicatorInflightMsgs = maxReplicatorInflightMsgs;
        return this;
    }

    public Integer getDisruptorBufferSize() {
        return disruptorBufferSize;
    }

    public ServerRaftProperties setDisruptorBufferSize(Integer disruptorBufferSize) {
        this.disruptorBufferSize = disruptorBufferSize;
        return this;
    }

    public Integer getElectionTimeoutMs() {
        return electionTimeoutMs;
    }

    public ServerRaftProperties setElectionTimeoutMs(Integer electionTimeoutMs) {
        this.electionTimeoutMs = electionTimeoutMs;
        return this;
    }

    public boolean isReporterEnabled() {
        return reporterEnabled;
    }

    public ServerRaftProperties setReporterEnabled(boolean reporterEnabled) {
        this.reporterEnabled = reporterEnabled;
        return this;
    }

    public Integer getReporterInitialDelay() {
        return reporterInitialDelay;
    }

    public ServerRaftProperties setReporterInitialDelay(Integer reporterInitialDelay) {
        this.reporterInitialDelay = reporterInitialDelay;
        return this;
    }

    public Boolean getAutoJoin() {
        return autoJoin;
    }

    public ServerRaftProperties setAutoJoin(Boolean autoJoin) {
        this.autoJoin = autoJoin;
        return this;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public String getCompressor() {
        return compressor;
    }

    public void setCompressor(String compressor) {
        this.compressor = compressor;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

}
