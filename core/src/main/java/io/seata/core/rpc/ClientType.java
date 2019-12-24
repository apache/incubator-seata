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
package io.seata.core.rpc;

/**
 * The enum Client type.
 *
 * @author slievrly
 */
public enum ClientType {

    /**
     * The Tm.
     */
    // Transaction Manager client
    TM,

    /**
     * The Rm.
     */
    // Resource Manager client
    RM;

    /**
     * Get client type.
     *
     * @param ordinal the ordinal
     * @return the client type
     */
    public static ClientType get(byte ordinal) {
        return get((int)ordinal);
    }

    /**
     * Get client type.
     *
     * @param ordinal the ordinal
     * @return the client type
     */
    public static ClientType get(int ordinal) {
        for (ClientType clientType : ClientType.values()) {
            if (clientType.ordinal() == ordinal) {
                return clientType;
            }
        }
        throw new IllegalArgumentException("Unknown ClientType[" + ordinal + "]");
    }
}
