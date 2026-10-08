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
package org.apache.seata.rm;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.rm.datasource.xa.Holdable;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Driver;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataSourceResourceBehaviorTest {
    @Test
    void delegatesDataSourceConfigurationAndRejectsMissingSource() throws Exception {
        BaseDataSourceResource resource = mock(
                BaseDataSourceResource.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        assertThrows(UnsupportedOperationException.class, resource::getLogWriter);
        DataSource source = mock(DataSource.class);
        resource.dataSource = source;
        PrintWriter writer = new PrintWriter(new StringWriter());
        when(source.getLogWriter()).thenReturn(writer);
        when(source.getLoginTimeout()).thenReturn(15);
        Logger logger = Logger.getLogger("test");
        when(source.getParentLogger()).thenReturn(logger);
        assertSame(source, resource.getTargetDataSource());
        resource.setLogWriter(writer);
        resource.setLoginTimeout(15);
        verify(source).setLogWriter(writer);
        verify(source).setLoginTimeout(15);
        assertSame(writer, resource.getLogWriter());
        assertEquals(15, resource.getLoginTimeout());
        assertSame(logger, resource.getParentLogger());
        assertSame(resource, resource.unwrap(DataSource.class));
        assertTrue(resource.isWrapperFor(DataSource.class));
        assertFalse(resource.isWrapperFor(null));
        assertNull(resource.unwrap(null));
        assertNull(resource.unwrap(Driver.class));
        resource.setResourceId("resource");
        resource.setResourceGroupId("group");
        resource.setBranchType(BranchType.XA);
        resource.setDbType("mysql");
        Driver driver = mock(Driver.class);
        resource.setDriver(driver);
        assertEquals("resource", resource.getResourceId());
        assertEquals("group", resource.getResourceGroupId());
        assertEquals(BranchType.XA, resource.getBranchType());
        assertEquals("mysql", resource.getDbType());
        assertSame(driver, resource.getDriver());
    }

    @Test
    void tracksHeldConnectionsAndDetectsMismatchedOwnership() {
        BaseDataSourceResource<Holdable> resource = mock(
                BaseDataSourceResource.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        Holdable value = mock(Holdable.class);
        assertNull(resource.hold("key", value));
        verify(value).setHeld(true);
        assertSame(value, resource.lookup("key"));
        when(value.isHeld()).thenReturn(true);
        assertSame(value, resource.hold("key", value));
        assertThrows(ShouldNeverHappenException.class, () -> resource.hold("other", value));
        assertSame(value, resource.release("key", value));
        verify(value).setHeld(false);
        assertTrue(resource.getKeeper().isEmpty());
        assertThrows(ShouldNeverHappenException.class, () -> resource.release("missing", value));
        resource.setShouldBeHeld(true);
        assertTrue(resource.isShouldBeHeld());
        String xid = "resource-behavior-test";
        try {
            BaseDataSourceResource.setBranchStatus(xid, BranchStatus.PhaseTwo_Committed);
            assertEquals(BranchStatus.PhaseTwo_Committed, BaseDataSourceResource.getBranchStatus(xid));
            BaseDataSourceResource.remove("");
            BaseDataSourceResource.remove(null);
        } finally {
            BaseDataSourceResource.remove(xid);
        }
        assertNull(BaseDataSourceResource.getBranchStatus(xid));
    }
}
