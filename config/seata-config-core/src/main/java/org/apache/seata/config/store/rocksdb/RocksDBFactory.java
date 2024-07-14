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
package org.apache.seata.config.store.rocksdb;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * The RocksDB Factory
 *
 */
public class RocksDBFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBFactory.class);

    private static volatile RocksDB instance = null;

    static {
        RocksDB.loadLibrary();
    }

    public static RocksDB getInstance(String dbPath, Options options) {
        if (instance == null) {
            synchronized (RocksDBFactory.class) {
                if (instance == null) {
                    instance = build(dbPath,options);
                }
            }
        }
        return instance;
    }

    private static RocksDB build(String dbPath, Options options) {
        try {
            checkPath(dbPath);
            return RocksDB.open(options, dbPath);
        }catch (RocksDBException | IOException e){
            LOGGER.error("RocksDB open error: {}", e.getMessage(), e);
            return null;
        }
    }


    public static synchronized void close() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    public static synchronized void destroy(String dbPath, Options options) {
        close();
        try {
            RocksDB.destroyDB(dbPath, options);
        }catch (RocksDBException e){
            LOGGER.error("RocksDB destroy error: {}",e.getMessage(),e);
        }
    }

    private static void checkPath(String dbPath) throws IOException {
        File directory = new File(dbPath);
        String message;
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                message = "File " + directory + " exists and is not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else if (!directory.mkdirs() && !directory.isDirectory()) {
            message = "Unable to create directory " + directory;
            throw new IOException(message);
        }
    }
}
