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
package io.seata.common.metadata;

/**
 * @author funkye
 */
public enum ClusterRole {

    /**
     * raft mode leader
     */
    LEADER(0),
    /**
     * raft mode follower
     */
    FOLLOWER(1),
    /**
     * raft mode learner
     */
    LEARNER(2),
    /**
     * cluster mode member
     */
    MEMBER(3);
    
    private int roleCode;

    ClusterRole(int roleCode) {
        this.roleCode = roleCode;
    }

    public int getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(int roleCode) {
        this.roleCode = roleCode;
    }

}
