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
package org.apache.seata.server.store;

/**
 * The file store engine inside file mode.
 */
public enum FileStoreEngine {
    /**
     * The default file log engine.
     */
    FILE("file"),
    /**
     * The RocksDB engine.
     */
    ROCKSDB("rocksdb");

    private final String name;

    FileStoreEngine(String name) {
        this.name = name;
    }

    public static FileStoreEngine get(String name) {
        for (FileStoreEngine engine : FileStoreEngine.values()) {
            if (engine.getName().equalsIgnoreCase(name)) {
                return engine;
            }
        }
        throw new IllegalArgumentException("unknown file store engine:" + name);
    }

    public String getName() {
        return name;
    }
}
