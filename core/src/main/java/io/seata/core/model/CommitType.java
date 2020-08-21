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
package io.seata.core.model;

/**
 * The enum of two phase commit type.
 *
 * @author wang.liang
 */
public enum CommitType {

    /**
     * The sync commit.
     */
    SyncCommit(0),

    /**
     * The async commit.
     */
    AsyncCommit(1),

    // This type will be provided later
    ///**
    // * The lazy commit.
    // */
    //LazyCommit(2),

    /**
     * The no commit.
     */
    NoCommit(3),
    ;

    private int code;

    CommitType(int code) {
        this.code = code;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get commit type
     *
     * @param code the code
     * @return the commit type
     */
    public static CommitType get(byte code) {
        return get((int) code);
    }

    /**
     * Get commit type.
     *
     * @param code the code
     * @return the commit type
     */
    public static CommitType get(int code) {
        for (CommitType t : CommitType.values()) {
            if (t.getCode() == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown type:" + code);
    }

    /**
     * Get the default by branch type
     *
     * @param branchType the branch type
     * @return the default commit type
     */
    public static CommitType getDefault(BranchType branchType) {
        if (branchType == BranchType.AT) {
            return AsyncCommit;
        } else if (branchType == BranchType.SAGA) {
            return NoCommit;
        } else {
            return SyncCommit;
        }
    }
}
