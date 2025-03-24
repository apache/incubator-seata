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
package org.apache.seata.core.model;

/**
 * The enum Branch type.
 *
 */
public enum BranchType {

    /**
     * The At.
     */
    // AT Branch
    AT,
    
    /**
     * The TCC.
     */
    TCC,

    /**
     * The SAGA.
     */
    SAGA,

    /**
     * The XA.
     */
    XA,

    /**
     * The SAGA_ANNOTATION.
     */
    SAGA_ANNOTATION;

    /**
     * Get branch type.
     *
     * @param ordinal the ordinal
     * @return the branch type
     */
    public static BranchType get(byte ordinal) {
        return get((int)ordinal);
    }

    /**
     * Get branch type.
     *
     * @param ordinal the ordinal
     * @return the branch type
     */
    public static BranchType get(int ordinal) {
        for (BranchType branchType : values()) {
            if (branchType.ordinal() == ordinal) {
                return branchType;
            }
        }
        throw new IllegalArgumentException("Unknown BranchType[" + ordinal + "]");
    }

    /**
     * Get branch type.
     *
     * @param name the name
     * @return the branch type
     */
    public static BranchType get(String name) {
        for (BranchType branchType : values()) {
            if (branchType.name().equalsIgnoreCase(name)) {
                return branchType;
            }
        }
        throw new IllegalArgumentException("Unknown BranchType[" + name + "]");
    }
}
