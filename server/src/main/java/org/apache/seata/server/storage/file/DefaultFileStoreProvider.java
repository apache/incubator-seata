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
package org.apache.seata.server.storage.file;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.server.storage.file.lock.DefaultFileLockStore;
import org.apache.seata.server.storage.file.session.FileSessionManager;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.apache.seata.server.storage.file.spi.FileStoreContext;
import org.apache.seata.server.storage.file.spi.FileStoreProvider;
import org.apache.seata.server.storage.file.spi.FileStoreRuntime;

import java.io.IOException;

@LoadLevel(name = "default")
public class DefaultFileStoreProvider implements FileStoreProvider {

    @Override
    public FileStoreRuntime open(FileStoreContext context) {
        FileSessionManager sessionManager = null;
        try {
            sessionManager = createSessionManager(context);
            FileLockStore lockStore = createLockStore();
            return new DefaultFileStoreRuntime(sessionManager, lockStore);
        } catch (IOException e) {
            StoreException failure = new StoreException(e, "open default file store runtime failed");
            closeSessionManager(sessionManager, failure);
            throw failure;
        } catch (RuntimeException | Error failure) {
            closeSessionManager(sessionManager, failure);
            throw failure;
        }
    }

    protected FileSessionManager createSessionManager(FileStoreContext context) throws IOException {
        return new FileSessionManager(
                context.getRootSessionManagerName(),
                context.getSessionStorePath().toString());
    }

    protected FileLockStore createLockStore() {
        return new DefaultFileLockStore();
    }

    private static void closeSessionManager(FileSessionManager sessionManager, Throwable failure) {
        if (sessionManager == null) {
            return;
        }
        try {
            sessionManager.destroy();
        } catch (RuntimeException | Error cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
