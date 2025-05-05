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
package org.apache.seata.namingserver;


import org.apache.seata.namingserver.listener.ClusterChangeEvent;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@SpringBootTest
public class ClusterWatcherManagerTest {

    private ClusterWatcherManager clusterWatcherManager;

    @Mock
    private AsyncContext asyncContext;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    private final String TEST_GROUP = "testGroup";
    private final int TEST_TIMEOUT = 5000;
    private final Long TEST_TERM = 1000L;
    private final String TEST_CLIENT_ENDPOINT = "127.0.0.1";

    @BeforeEach
    void setUp() {
        clusterWatcherManager = new ClusterWatcherManager();
        Mockito.when(asyncContext.getResponse()).thenReturn(response);
        Mockito.when(asyncContext.getRequest()).thenReturn(request);
        Mockito.when(request.getRemoteAddr()).thenReturn(TEST_CLIENT_ENDPOINT);

        Map<String, Queue<Watcher<?>>> watchers = (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");
        Map<String, Long> groupUpdateTime = (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TIME");

        watchers.clear();
        groupUpdateTime.clear();
    }

    @Test
    void testInit() {
        clusterWatcherManager.init();
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) ReflectionTestUtils
                .getField(clusterWatcherManager, "scheduledThreadPoolExecutor");

        assertNotNull(scheduledThreadPoolExecutor);
        assertFalse(scheduledThreadPoolExecutor.isShutdown());
    }

    @Test
    void testRegistryNewWatcher() {
        Watcher<AsyncContext> watcher = new Watcher<>(TEST_GROUP, asyncContext, TEST_TIMEOUT, TEST_TERM, TEST_CLIENT_ENDPOINT);
        clusterWatcherManager.registryWatcher(watcher);

        Map<String, Queue<Watcher<?>>> watchers = (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");

        assertNotNull(watchers);
        assertTrue(watchers.containsKey(TEST_GROUP));
        assertEquals(1, watchers.get(TEST_GROUP).size());
        assertFalse(watcher.isDone());
    }

    @Test
    void testRegistryWatcherOldTerm() {
        Map<String, Long> groupUpdateTime = (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TIME");
        groupUpdateTime.put(TEST_GROUP, TEST_TERM + 10);

        Watcher<AsyncContext> watcher = new Watcher<>(TEST_GROUP, asyncContext, TEST_TIMEOUT, TEST_TERM, TEST_CLIENT_ENDPOINT);
        clusterWatcherManager.registryWatcher(watcher);

        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(asyncContext).complete();
        assertTrue(watcher.isDone());

        Map<String, Queue<Watcher<?>>> watchers = (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");

        assertFalse(watchers.containsKey(TEST_GROUP));
        assertNull(watchers.get(TEST_GROUP));
    }

    @Test
    void testOnEventChange() {
        Watcher<AsyncContext> watcher = new Watcher<>(TEST_GROUP, asyncContext, TEST_TIMEOUT, TEST_TERM, TEST_CLIENT_ENDPOINT);
        clusterWatcherManager.registryWatcher(watcher);
        Map<String, Queue<Watcher<?>>> watchers = (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");
        Map<String, Long> updateTime = (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TIME");

        assertNotNull(watchers);
        assertNotNull(updateTime);

        ClusterChangeEvent zeroTermEvent = new ClusterChangeEvent(this, TEST_GROUP, 0);
        clusterWatcherManager.onChangeEvent(zeroTermEvent);

        assertEquals(0, updateTime.size());
        assertFalse(watcher.isDone());
        assertTrue(watchers.containsKey(TEST_GROUP));
        assertNotNull(watchers.get(TEST_GROUP));
        assertEquals(1, watchers.get(TEST_GROUP).size());

        ClusterChangeEvent event = new ClusterChangeEvent(this, TEST_GROUP, TEST_TERM + 1);
        clusterWatcherManager.onChangeEvent(event);

        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(asyncContext).complete();

        assertEquals(1, updateTime.size());
        assertEquals(TEST_TERM + 1, updateTime.get(TEST_GROUP));
        assertTrue(watcher.isDone());
        assertFalse(watchers.containsKey(TEST_GROUP));
        assertNull(watchers.get(TEST_GROUP));
    }

    @Test
    void testGetWatcherIpList() {
        Watcher<AsyncContext> watcher1 = new Watcher<>(TEST_GROUP, asyncContext, TEST_TIMEOUT, TEST_TERM, "127.0.0.1");
        Watcher<AsyncContext> watcher2 = new Watcher<>(TEST_GROUP, asyncContext, TEST_TIMEOUT, TEST_TERM, "127.0.0.1");
        Watcher<AsyncContext> watcher3 = new Watcher<>(TEST_GROUP, asyncContext, TEST_TIMEOUT, TEST_TERM, "192.168.1.1");

        clusterWatcherManager.registryWatcher(watcher1);
        clusterWatcherManager.registryWatcher(watcher2);
        clusterWatcherManager.registryWatcher(watcher3);
        List<String> watcherIpList = clusterWatcherManager.getWatcherIpList(TEST_GROUP);

        assertNotNull(watcherIpList);
        assertEquals(2, watcherIpList.size());
        assertTrue(watcherIpList.contains("127.0.0.1"));
        assertTrue(watcherIpList.contains("192.168.1.1"));
    }

    @Test
    void testGetWatchVGroupList() {
        Watcher<AsyncContext> watcher1 = new Watcher<>("VGroup1", asyncContext, TEST_TIMEOUT, TEST_TERM, "127.0.0.1");
        Watcher<AsyncContext> watcher2 = new Watcher<>("VGroup1", asyncContext, TEST_TIMEOUT, TEST_TERM, "127.0.0.2");
        Watcher<AsyncContext> watcher3 = new Watcher<>("VGroup2", asyncContext, TEST_TIMEOUT, TEST_TERM, "192.168.1.1");

        clusterWatcherManager.registryWatcher(watcher1);
        clusterWatcherManager.registryWatcher(watcher2);
        clusterWatcherManager.registryWatcher(watcher3);

        List<String> watchVGroupList = clusterWatcherManager.getWatchVGroupList();

        assertNotNull(watchVGroupList);
        assertEquals(2, watchVGroupList.size());
        assertTrue(watchVGroupList.contains("VGroup1"));
        assertTrue(watchVGroupList.contains("VGroup2"));
    }

    @Test
    void testGetTermByvGroup() {
        Map<String, Long> groupUpdateTime = (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TIME");

        assertNotNull(groupUpdateTime);

        groupUpdateTime.put(TEST_GROUP, TEST_TERM);
        Long term1 = clusterWatcherManager.getTermByvGroup(TEST_GROUP);
        Long term2 = clusterWatcherManager.getTermByvGroup("NotExist");

        assertEquals(TEST_TERM, term1);
        assertEquals(0L, term2);
    }
}
