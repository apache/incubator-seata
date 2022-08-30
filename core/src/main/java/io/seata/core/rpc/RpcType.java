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
 * @author goodboycoder
 */
public enum RpcType {
    /**
     * netty
     */
    NETTY("netty"),
    /**
     * grpc
     */
    GRPC("grpc");

    public final String name;

    RpcType(String name) {
        this.name = name;
    }

    /**
     * Gets rpc type by name.
     *
     * @param name the name
     * @return the type by name
     */
    public static RpcType getTypeByName(String name) {
        for (RpcType rpcType : values()) {
            if (rpcType.name().equalsIgnoreCase(name)) {
                return rpcType;
            }
        }
        throw new IllegalArgumentException("unknown rpc type:" + name);
    }
}
