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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Map;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.rm.datasource.xa.Holdable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseDataSourceResourceTest {

  static class DummyHoldable implements Holdable {

    private boolean held = false;

    @Override
    public boolean isHeld() {
      return held;
    }

    @Override
    public void setHeld(boolean held) {
      this.held = held;
    }

    @Override
    public boolean shouldBeHeld() {
      return true;
    }
  }

  static class DummyResource extends BaseDataSourceResource<DummyHoldable> {

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public java.sql.Connection getConnection() {
      return null;
    }

    @Override
    public java.sql.Connection getConnection(String username, String password) {
      return null;
    }

  }

  private DummyResource resource;

  @BeforeEach
  void setUp() {
    resource = new DummyResource();
  }

  @Test
  void testHoldReleaseSuccess() {
    DummyHoldable h = new DummyHoldable();
    assertNull(resource.hold("k1", h));
    assertEquals(h, resource.lookup("k1"));
    assertTrue(h.isHeld());

    DummyHoldable removed = resource.release("k1", h);
    assertEquals(h, removed);
    assertFalse(h.isHeld());
  }

  @Test
  void testHoldTwiceShouldFail() {
    DummyHoldable h1 = new DummyHoldable();
    DummyHoldable h2 = new DummyHoldable();
    resource.hold("k", h1);
    h2.setHeld(true);
    assertThrows(ShouldNeverHappenException.class, () -> resource.hold("k", h2));
  }

  @Test
  void testReleaseWrongObject() {
    DummyHoldable h1 = new DummyHoldable();
    DummyHoldable h2 = new DummyHoldable();
    resource.hold("k", h1);
    assertThrows(ShouldNeverHappenException.class, () -> resource.release("k", h2));
  }

  @Test
  void testBranchStatusCacheOps() {
    BaseDataSourceResource.setBranchStatus("x1", BranchStatus.PhaseOne_Done);
    assertEquals(BranchStatus.PhaseOne_Done, BaseDataSourceResource.getBranchStatus("x1"));
    BaseDataSourceResource.remove("x1");
    assertNull(BaseDataSourceResource.getBranchStatus("x1"));
  }

  @Test
  void testResourceIdGroupSetters() {
    resource.setResourceId("resId");
    resource.setResourceGroupId("groupId");
    resource.setDbType("mysql");

    assertEquals("resId", resource.getResourceId());
    assertEquals("groupId", resource.getResourceGroupId());
    assertEquals("mysql", resource.getDbType());
  }

  @Test
  void testShouldBeHeldFlag() {
    resource.setShouldBeHeld(true);
    assertTrue(resource.isShouldBeHeld());
  }

  @Test
  void testDriverSetter() {
    assertNull(resource.getDriver());
    assertDoesNotThrow(() -> resource.setDriver(null));
  }

  @Test
  void testWrapperMethods() throws SQLException {
    assertTrue(resource.isWrapperFor(DummyResource.class));
    assertSame(resource, resource.unwrap(DummyResource.class));
  }

  @Test
  void testKeeperAccess() {
    DummyHoldable h = new DummyHoldable();
    resource.hold("a", h);
    Map<String, DummyHoldable> keeper = resource.getKeeper();
    assertTrue(keeper.containsKey("a"));
  }
}
