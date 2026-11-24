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
package org.apache.seata.common.json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link JsonAllowlistManager}
 */
public class JsonAllowlistManagerTest {

    @AfterEach
    void tearDown() {

        JsonAllowlistManager.getInstance().clearUserAllowlist();
    }

    @Test
    public void testBuiltinAllowlist_primitiveWrappers() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThat(manager.isAllowed("java.lang.String")).isTrue();
        assertThat(manager.isAllowed("java.lang.Integer")).isTrue();
        assertThat(manager.isAllowed("java.lang.Long")).isTrue();
        assertThat(manager.isAllowed("java.lang.Boolean")).isTrue();
        assertThat(manager.isAllowed("java.lang.Double")).isTrue();
        assertThat(manager.isAllowed("java.lang.Float")).isTrue();
        assertThat(manager.isAllowed("java.lang.Byte")).isTrue();
        assertThat(manager.isAllowed("java.lang.Short")).isTrue();
        assertThat(manager.isAllowed("java.lang.Character")).isTrue();
        assertThat(manager.isAllowed("java.lang.Number")).isTrue();
        assertThat(manager.isAllowed("java.math.BigDecimal")).isTrue();
        assertThat(manager.isAllowed("java.math.BigInteger")).isTrue();
    }

    @Test
    public void testBuiltinAllowlist_arrays() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        // 1D primitive arrays
        assertThat(manager.isAllowed("[B")).isTrue();
        assertThat(manager.isAllowed("[I")).isTrue();
        assertThat(manager.isAllowed("[J")).isTrue();
        assertThat(manager.isAllowed("[Z")).isTrue();
        assertThat(manager.isAllowed("[C")).isTrue();
        assertThat(manager.isAllowed("[S")).isTrue();
        assertThat(manager.isAllowed("[F")).isTrue();
        assertThat(manager.isAllowed("[D")).isTrue();

        // Multi-dimensional primitive arrays
        assertThat(manager.isAllowed("[[I")).isTrue();
        assertThat(manager.isAllowed("[[Z")).isTrue();
        assertThat(manager.isAllowed("[[B")).isTrue();
        assertThat(manager.isAllowed("[[J")).isTrue();
        assertThat(manager.isAllowed("[[[D")).isTrue();
        assertThat(manager.isAllowed("[[[F")).isTrue();

        // Object arrays
        assertThat(manager.isAllowed("[Ljava.lang.String;")).isTrue();
        assertThat(manager.isAllowed("[Ljava.lang.Object;")).isTrue();
    }

    @Test
    public void testBuiltinAllowlist_dateTime() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThat(manager.isAllowed("java.util.Date")).isTrue();
        assertThat(manager.isAllowed("java.sql.Timestamp")).isTrue();
        assertThat(manager.isAllowed("java.time.LocalDateTime")).isTrue();
        assertThat(manager.isAllowed("java.time.LocalDate")).isTrue();
        assertThat(manager.isAllowed("java.time.Instant")).isTrue();
        assertThat(manager.isAllowed("java.time.ZonedDateTime")).isTrue();
    }

    @Test
    public void testBuiltinAllowlist_collections() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThat(manager.isAllowed("java.util.ArrayList")).isTrue();
        assertThat(manager.isAllowed("java.util.LinkedList")).isTrue();
        assertThat(manager.isAllowed("java.util.HashMap")).isTrue();
        assertThat(manager.isAllowed("java.util.LinkedHashMap")).isTrue();
        assertThat(manager.isAllowed("java.util.HashSet")).isTrue();
        assertThat(manager.isAllowed("java.util.TreeMap")).isTrue();
        assertThat(manager.isAllowed("java.util.concurrent.ConcurrentHashMap")).isTrue();
    }

    @Test
    public void testBuiltinAllowlist_seataPrefix() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThat(manager.isAllowed("org.apache.seata.core.model.BranchType")).isTrue();
        assertThat(manager.isAllowed("io.seata.saga.engine.mock.DemoService$People"))
                .isTrue();
        assertThat(manager.isAllowed("org.apache.seata.rm.datasource.undo.UndoLogParser"))
                .isTrue();
        assertThat(manager.isAllowed("org.apache.seata.common.json.JsonAllowlistManager"))
                .isTrue();
    }

    @Test
    public void testBuiltinAllowlist_objectArrayDescriptorsByPrefix() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThat(manager.isAllowed("[Lorg.apache.seata.common.json.JsonAllowlistManager;"))
                .isTrue();
        assertThat(manager.isAllowed("[Lio.seata.saga.engine.mock.DemoService$People;"))
                .isTrue();
        assertThat(manager.isAllowed("[[Lio.seata.saga.engine.mock.DemoService$People;"))
                .isTrue();
        assertThat(manager.isAllowed("[Lcom.malicious.EvilClass;")).isFalse();
    }

    @Test
    public void testBuiltinAllowlist_objectArrayCanonicalNameByPrefix() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThat(manager.isAllowed("io.seata.saga.engine.mock.DemoService$People[]"))
                .isTrue();
        assertThat(manager.isAllowed("io.seata.saga.engine.mock.DemoService$People[][]"))
                .isTrue();
        assertThat(manager.isAllowed("com.malicious.EvilClass[]")).isFalse();
    }

    @Test
    public void testBuiltinAllowlist_notAllowed() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThat(manager.isAllowed("com.sun.rowset.JdbcRowSetImpl")).isFalse();
        assertThat(manager.isAllowed("java.lang.Runtime")).isFalse();
        assertThat(manager.isAllowed("java.lang.ProcessBuilder")).isFalse();
        assertThat(manager.isAllowed("javax.naming.InitialContext")).isFalse();
        assertThat(manager.isAllowed("com.example.MaliciousClass")).isFalse();
    }

    @Test
    public void testLoadUserAllowlist_exactMatch() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.loadUserAllowlist("com.example.MyClass,com.example.AnotherClass");

        assertThat(manager.isAllowed("com.example.MyClass")).isTrue();
        assertThat(manager.isAllowed("com.example.AnotherClass")).isTrue();
        assertThat(manager.isAllowed("com.example.NotInList")).isFalse();
    }

    @Test
    public void testLoadUserAllowlist_prefixMatch() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.loadUserAllowlist("com.company.model.,com.company.dto.");

        assertThat(manager.isAllowed("com.company.model.User")).isTrue();
        assertThat(manager.isAllowed("com.company.model.Order")).isTrue();
        assertThat(manager.isAllowed("com.company.dto.UserDTO")).isTrue();
        assertThat(manager.isAllowed("com.company.service.UserService")).isFalse();
    }

    @Test
    public void testLoadUserAllowlist_mixedMatch() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.loadUserAllowlist("com.company.model.,com.thirdparty.SpecificClass");

        assertThat(manager.isAllowed("com.company.model.User")).isTrue();
        assertThat(manager.isAllowed("com.company.model.sub.NestedClass")).isTrue();
        assertThat(manager.isAllowed("com.thirdparty.SpecificClass")).isTrue();
        assertThat(manager.isAllowed("com.thirdparty.OtherClass")).isFalse();
    }

    @Test
    public void testLoadUserAllowlist_withSpaces() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.loadUserAllowlist("  com.example.Class1  ,  com.example.Class2  ");

        assertThat(manager.isAllowed("com.example.Class1")).isTrue();
        assertThat(manager.isAllowed("com.example.Class2")).isTrue();
    }

    @Test
    public void testLoadUserAllowlist_emptyEntries() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.loadUserAllowlist("com.example.Class1,,com.example.Class2,");

        assertThat(manager.isAllowed("com.example.Class1")).isTrue();
        assertThat(manager.isAllowed("com.example.Class2")).isTrue();
    }

    @Test
    public void testLoadUserAllowlist_nullOrEmpty() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.loadUserAllowlist(null);
        manager.loadUserAllowlist("");

        assertThat(manager.isAllowed("java.lang.String")).isTrue();
    }

    @Test
    public void testAddUserClass() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.addUserClass("com.example.DynamicClass");

        assertThat(manager.isAllowed("com.example.DynamicClass")).isTrue();
    }

    @Test
    public void testAddUserPrefix() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.addUserPrefix("com.dynamic.pkg.");

        assertThat(manager.isAllowed("com.dynamic.pkg.Class1")).isTrue();
        assertThat(manager.isAllowed("com.dynamic.pkg.sub.Class2")).isTrue();
    }

    @Test
    public void testClearUserAllowlist() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.loadUserAllowlist("com.example.ToBeCleared");
        assertThat(manager.isAllowed("com.example.ToBeCleared")).isTrue();

        manager.clearUserAllowlist();
        assertThat(manager.isAllowed("com.example.ToBeCleared")).isFalse();

        assertThat(manager.isAllowed("java.lang.String")).isTrue();
    }

    @Test
    public void testCheckClass_allowed() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.checkClass("java.lang.String");
        manager.checkClass("java.util.ArrayList");
        manager.checkClass("org.apache.seata.core.model.BranchType");
    }

    @Test
    public void testCheckClass_notAllowed() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThatThrownBy(() -> manager.checkClass("com.malicious.EvilClass"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not in JSON deserialization allowlist")
                .hasMessageContaining("com.malicious.EvilClass")
                .hasMessageContaining("seata.json.allowlist");
    }

    @Test
    public void testCheckClass_userAllowed() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.loadUserAllowlist("com.myapp.model.");

        manager.checkClass("com.myapp.model.User");
    }

    @Test
    public void testIsAllowed_null() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThat(manager.isAllowed(null)).isFalse();
    }

    @Test
    public void testCheckClass_null() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        assertThatThrownBy(() -> manager.checkClass(null)).isInstanceOf(SecurityException.class);
    }

    @Test
    public void testAddUserClass_nullOrEmpty() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.addUserClass(null);
        manager.addUserClass("");
    }

    @Test
    public void testAddUserPrefix_nullOrEmpty() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.addUserPrefix(null);
        manager.addUserPrefix("");
    }

    @Test
    public void testSingleton() {
        JsonAllowlistManager instance1 = JsonAllowlistManager.getInstance();
        JsonAllowlistManager instance2 = JsonAllowlistManager.getInstance();

        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    public void testCacheWorks() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        boolean result1 = manager.isAllowed("java.lang.String");

        boolean result2 = manager.isAllowed("java.lang.String");

        assertThat(result1).isTrue();
        assertThat(result2).isTrue();
    }

    @Test
    public void testCacheClearedOnLoadUserAllowlist() {
        JsonAllowlistManager manager = JsonAllowlistManager.getInstance();

        manager.isAllowed("com.example.CacheTest");

        manager.loadUserAllowlist("com.example.CacheTest");

        assertThat(manager.isAllowed("com.example.CacheTest")).isTrue();
    }
}
