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
package io.seata.rm.datasource;

import com.alibaba.druid.mock.MockSavepoint;
import io.seata.rm.datasource.undo.SQLUndoLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Savepoint;
import java.util.List;

/**
 * ConnectionContextProxy test
 *
 * @author chd
 */
public class ConnectionContextProxyTest {
    ConnectionContext connectionContext = new ConnectionContext();

    @Test
    public void testBuildLockKeys() throws Exception {
        connectionContext.appendLockKey("abc");
        connectionContext.appendLockKey("bcd");

        Assertions.assertTrue(connectionContext.hasLockKey());
        Assertions.assertEquals(connectionContext.buildLockKeys(), "bcd;abc");
    }

    @Test
    public void testAppendUndoItem() {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        connectionContext.appendUndoItem(sqlUndoLog);
        SQLUndoLog sqlUndoLog1 = new SQLUndoLog();
        connectionContext.appendUndoItem(sqlUndoLog1);

        Assertions.assertTrue(connectionContext.hasUndoLog());
        Assertions.assertEquals(connectionContext.getUndoItems().size(), 2);
        Assertions.assertSame(connectionContext.getUndoItems().get(0), sqlUndoLog);
        Assertions.assertSame(connectionContext.getUndoItems().get(1), sqlUndoLog1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAfterSavepoints() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Savepoint sp1 = new MockSavepoint();
        Savepoint sp2 = new MockSavepoint();
        Savepoint sp3 = new MockSavepoint();
        connectionContext.appendSavepoint(sp1);
        connectionContext.appendSavepoint(sp2);
        connectionContext.appendSavepoint(sp3);

        Method m = ConnectionContext.class.getDeclaredMethod("getAfterSavepoints", Savepoint.class);
        m.setAccessible(true);

        List<Savepoint> invoke = (List<Savepoint>) m.invoke(connectionContext, new Object[]{null});
        Assertions.assertEquals(invoke.size(), 3);

        invoke = (List<Savepoint>) m.invoke(connectionContext, sp2);
        Assertions.assertEquals(invoke.size(), 2);
    }

    @Test
    public void testBindAndUnbind() {
        connectionContext.bind("test-xid");
        Assertions.assertTrue(connectionContext.inGlobalTransaction());

        connectionContext.reset();

        connectionContext.setGlobalLockRequire(true);
        Assertions.assertTrue(connectionContext.isGlobalLockRequire());
    }

    @Test
    public void testRemoveSavepoint() {
        Savepoint sp1 = new MockSavepoint();
        connectionContext.appendSavepoint(sp1);
        connectionContext.appendUndoItem(new SQLUndoLog());
        connectionContext.appendLockKey("sp1-lock-key");

        Savepoint sp2 = new MockSavepoint();
        connectionContext.appendSavepoint(sp2);

        Savepoint sp3 = new MockSavepoint();
        connectionContext.appendSavepoint(sp3);
        connectionContext.appendLockKey("sp3-lock-key");
        connectionContext.appendUndoItem(new SQLUndoLog());

        Assertions.assertEquals(connectionContext.getUndoItems().size(), 2);
        Assertions.assertEquals(connectionContext.buildLockKeys(), "sp3-lock-key;sp1-lock-key");


        connectionContext.removeSavepoint(sp3);
        Assertions.assertEquals(connectionContext.getUndoItems().size(), 1);
        Assertions.assertEquals(connectionContext.buildLockKeys(), "sp1-lock-key");

        connectionContext.removeSavepoint(null);
        Assertions.assertEquals(connectionContext.getUndoItems().size(), 0);
        Assertions.assertNull(connectionContext.buildLockKeys());
    }


    @Test
    public void testReleaseSavepoint() {
        Savepoint sp1 = new MockSavepoint();
        connectionContext.appendSavepoint(sp1);
        connectionContext.appendUndoItem(new SQLUndoLog());
        connectionContext.appendLockKey("sp1-lock-key");

        Savepoint sp2 = new MockSavepoint();
        connectionContext.appendSavepoint(sp2);

        Savepoint sp3 = new MockSavepoint();
        connectionContext.appendSavepoint(sp3);
        connectionContext.appendLockKey("sp3-lock-key");
        connectionContext.appendUndoItem(new SQLUndoLog());

        Assertions.assertEquals(connectionContext.getUndoItems().size(), 2);
        Assertions.assertEquals(connectionContext.buildLockKeys(), "sp3-lock-key;sp1-lock-key");


        connectionContext.releaseSavepoint(sp3);
        Assertions.assertEquals(connectionContext.getUndoItems().size(), 2);
        Assertions.assertEquals(connectionContext.buildLockKeys(), "sp3-lock-key;sp1-lock-key");

        connectionContext.releaseSavepoint(null);
        Assertions.assertEquals(connectionContext.getUndoItems().size(), 2);
        Assertions.assertEquals(connectionContext.buildLockKeys(), "sp3-lock-key;sp1-lock-key");
    }



    @AfterEach
    public void clear() {
        connectionContext.reset();
    }
}
