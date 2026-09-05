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
package org.apache.seata.server.storage.file.spi;

import org.apache.seata.server.session.SessionManager;

/**
 * Runtime resources for a file-mode storage provider.
 */
public interface FileStoreRuntime extends AutoCloseable {

    /**
     * Gets the session manager owned by this runtime.
     *
     * @return the session manager
     */
    SessionManager sessionManager();

    /**
     * Gets the lock store owned by this runtime.
     *
     * @return the lock store
     */
    FileLockStore lockStore();

    /**
     * Recovers sessions through the supplied consumer.
     *
     * @param recoveryConsumer the recovery consumer
     */
    void recover(SessionRecoveryConsumer recoveryConsumer);

    /**
     * Starts background services owned by this runtime.
     */
    void startBackgroundServices();

    /**
     * Closes resources owned by this runtime.
     */
    @Override
    void close();
}
