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

/**
 * Provider for a file-mode storage runtime.
 *
 * <p>The default engine preserves the legacy {@code SessionManager} provider named {@code file}
 * (with {@code String, String} constructor arguments) and the no-argument {@code LockManager} SPI.
 * Session and lock modes remain independently selectable.
 *
 * <p>Opting into a non-default engine with FILE locks requires the selected file lock manager to
 * accept a {@link FileLockStore} constructor argument. The built-in file lock manager supports both
 * constructors. Existing custom lock managers can stay on the default engine unchanged, or add the
 * typed constructor and delegate operations to the supplied store when adopting a new engine.
 * The typed manager must not be initialized before its runtime is opened.
 */
public interface FileStoreProvider {

    /**
     * Opens a new runtime for the supplied context.
     *
     * @param context the file store context
     * @return a distinct file store runtime
     */
    FileStoreRuntime open(FileStoreContext context);
}
