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
package org.apache.seata.core.rpc;

/**
 * The enum Transport server type.
 *
 */
public enum TransportServerType {
    /**
     * Native transport server type.
     */
    NATIVE("native"),
    /**
     * Nio transport server type.
     */
    NIO("nio");

    /**
     * The Name.
     */
    public final String name;

    TransportServerType(String name) {
        this.name = name;
    }

    /**
     * Gets type.
     *
     * @param name the name
     * @return the type
     */
    public static TransportServerType getType(String name) {
        for (TransportServerType b : TransportServerType.values()) {
            if (b.name().equalsIgnoreCase(name)) {
                return b;
            }
        }
        throw new IllegalArgumentException("unknown type:" + name);
    }
}
