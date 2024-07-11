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
package org.apache.seata.common.util;

/**
 * The type Uuid generator.
 */
public class UUIDGenerator {

    private static volatile IdWorker idWorker;

    /**
     * generate UUID using snowflake algorithm
     *
     * @return UUID
     */
    public static long generateUUID() {
        if (idWorker == null) {
            synchronized (UUIDGenerator.class) {
                if (idWorker == null) {
                    init(null);
                }
            }
        }
        return idWorker.nextId();
    }

    /**
     * init IdWorker
     *
     * @param serverNode the server node id, consider as machine id in snowflake
     */
    public static void init(Long serverNode) {
        idWorker = new IdWorker(serverNode);
    }
}
