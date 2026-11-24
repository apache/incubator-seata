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
package org.apache.seata.server.storage.file.store;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.ServerRunner;
import org.apache.seata.server.session.*;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoBean(types = ServerRunner.class)
class FileStoreFailureUnitTest extends BaseSpringBootTest {
    @TempDir
    Path directory;

    @Test
    void rotationPreservesSessionsAndCreatesNewCurrentFile() throws Exception {
        SessionManager sessions = mock(SessionManager.class);
        GlobalSession global = GlobalSession.createGlobalSession("app", "group", "order", 1000);
        when(sessions.findGlobalSessions(any())).thenReturn(Collections.singletonList(global));
        Path file = directory.resolve("nested/session");
        FileTransactionStoreManager store = new FileTransactionStoreManager(file.toString(), sessions);
        try {
            assertTrue(store.writeSession(LogOperation.GLOBAL_ADD, global));
            assertTrue((Boolean) ReflectionTestUtils.invokeMethod(store, "saveHistory"));
            assertTrue(Files.exists(Paths.get(file + ".1")));
            assertEquals(
                    global.getXid(),
                    ((GlobalSession) store.readWriteStore(10, true).get(0).getSessionRequest()).getXid());
            assertFalse(store.hasRemaining(true));
            assertFalse(store.hasRemaining(false));
            assertThrows(StoreException.class, () -> store.readSession("xid"));
            assertThrows(StoreException.class, () -> store.readSession(new SessionCondition()));
            assertFalse(store.writeSession(LogOperation.GLOBAL_ADD, null));
        } finally {
            store.shutdown();
        }
    }

    @Test
    void incompleteFramesStopRecoveryWithoutInventingSessions() throws Exception {
        Path file = directory.resolve("session");
        FileTransactionStoreManager store =
                new FileTransactionStoreManager(file.toString(), mock(SessionManager.class));
        try {
            Files.write(file, new byte[] {0, 0});
            assertTrue(store.readWriteStore(10, false).isEmpty());
            ReflectionTestUtils.setField(store, "recoverCurrOffset", 0L);
            Files.write(file, ByteBuffer.allocate(5).putInt(20).put((byte) 1).array());
            assertTrue(store.readWriteStore(10, false).isEmpty());
            ReflectionTestUtils.setField(store, "recoverCurrOffset", 0L);
            Files.write(file, ByteBuffer.allocate(5).putInt(1).put((byte) 127).array());
            assertTrue(store.readWriteStore(10, false).isEmpty());
            assertNull(store.readWriteStore(10, true));
        } finally {
            store.shutdown();
        }
    }

    @Test
    void failedWritesAreRetriedAndReturnFalse() throws Exception {
        FileTransactionStoreManager store =
                new FileTransactionStoreManager(directory.resolve("session").toString(), mock(SessionManager.class));
        FileChannel original = (FileChannel) ReflectionTestUtils.getField(store, "currFileChannel");
        FileChannel failing = mock(FileChannel.class);
        when(failing.write(any(ByteBuffer.class))).thenThrow(new IOException("disk full"));
        try {
            ReflectionTestUtils.setField(store, "currFileChannel", failing);
            assertFalse(store.writeSession(
                    LogOperation.GLOBAL_ADD, GlobalSession.createGlobalSession("app", "group", "order", 1000)));
            verify(failing, times(5)).write(any(ByteBuffer.class));
        } finally {
            ReflectionTestUtils.setField(store, "currFileChannel", original);
            store.shutdown();
        }
    }

    @Test
    void workerDrainsFlushAndCloseRequestsOnStop() throws Exception {
        FileTransactionStoreManager store = mock(FileTransactionStoreManager.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(store, "stopping", true);
        FileChannel syncChannel = mock(FileChannel.class);
        FileChannel closeChannel = mock(FileChannel.class);
        RandomAccessFile file = mock(RandomAccessFile.class);
        FileTransactionStoreManager.WriteDataFileRunnable worker = store.new WriteDataFileRunnable();
        // Keep the synchronization check independent of counts shared by other file stores.
        FileTransactionStoreManager.SyncFlushRequest sync =
                spy(store.new SyncFlushRequest(Long.MAX_VALUE, syncChannel));
        FileTransactionStoreManager.AsyncFlushRequest async =
                spy(store.new AsyncFlushRequest(0, mock(FileChannel.class)));
        FileTransactionStoreManager.CloseFileRequest close =
                spy(new FileTransactionStoreManager.CloseFileRequest(closeChannel, file));

        worker.putRequest(sync);
        worker.putRequest(async);
        worker.putRequest(close);
        worker.run();

        verify(syncChannel).force(false);
        verify(sync).wakeup();
        verify(async).getCurFileChannel();
        verify(closeChannel).force(false);
        verify(file).close();
        verify(close).wakeup();

        FileChannel failingChannel = mock(FileChannel.class);
        doThrow(new IOException("force failed")).when(failingChannel).force(false);
        FileTransactionStoreManager.SyncFlushRequest failed =
                spy(store.new SyncFlushRequest(Long.MAX_VALUE, failingChannel));
        worker.putRequest(failed);
        assertDoesNotThrow(worker::run);
        verify(failingChannel).force(false);
        verify(failed).wakeup();
    }
}
