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
package org.apache.seata.server.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "undo_log",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "ux_undo_log",
                        columnNames = {"xid", "branch_id"}))
public class UndoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(nullable = false, length = 100)
    private String xid;

    @Column(nullable = false, length = 128)
    private String context;

    @Lob
    @Column(name = "rollback_info", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] rollbackInfo;

    @Column(name = "log_status", nullable = false)
    private Integer logStatus;

    @Column(name = "log_created", nullable = false)
    private LocalDateTime logCreated;

    @Column(name = "log_modified", nullable = false)
    private LocalDateTime logModified;

    @Column(length = 100)
    private String ext;

    // Constructors

    public UndoLog() {}

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public byte[] getRollbackInfo() {
        return rollbackInfo;
    }

    public void setRollbackInfo(byte[] rollbackInfo) {
        this.rollbackInfo = rollbackInfo;
    }

    public Integer getLogStatus() {
        return logStatus;
    }

    public void setLogStatus(Integer logStatus) {
        this.logStatus = logStatus;
    }

    public LocalDateTime getLogCreated() {
        return logCreated;
    }

    public void setLogCreated(LocalDateTime logCreated) {
        this.logCreated = logCreated;
    }

    public LocalDateTime getLogModified() {
        return logModified;
    }

    public void setLogModified(LocalDateTime logModified) {
        this.logModified = logModified;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    @Override
    public String toString() {
        return "UndoLog{" + "id="
                + id + ", branchId="
                + branchId + ", xid='"
                + xid + '\'' + ", logStatus="
                + logStatus + '}';
    }
}
