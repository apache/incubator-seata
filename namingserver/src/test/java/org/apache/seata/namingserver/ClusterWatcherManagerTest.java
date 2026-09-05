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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.namingserver.listener.ClusterChangeEvent;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.manager.ClusterWatcherManager;
import org.apache.seata.namingserver.metrics.NoOpNamingMetricsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClusterWatcherManagerTest {

    private ClusterWatcherManager clusterWatcherManager;

    @Mock
    private AsyncContext asyncContext;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final String testGroup = "testGroup";
    private final String testNamespace = "testNamespace";
    private final String testCluster = "testCluster";
    private final int testTimeout = 5000;
    private final Long testTerm = 1000L;
    private final String testClientEndpoint = "127.0.0.1";

    @BeforeEach
    void setUp() {
        clusterWatcherManager = new ClusterWatcherManager();
        // Inject dependencies to avoid null pointer
        ReflectionTestUtils.setField(clusterWatcherManager, "metricsManager", new NoOpNamingMetricsManager());
        ReflectionTestUtils.setField(clusterWatcherManager, "eventPublisher", eventPublisher);

        Mockito.when(asyncContext.getResponse()).thenReturn(response);
        Mockito.when(asyncContext.getRequest()).thenReturn(request);
        Mockito.when(request.getRemoteAddr()).thenReturn(testClientEndpoint);

        Map<String, Queue<Watcher<?>>> watchers =
                (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");
        Map<String, Long> groupUpdateTime =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");

        watchers.clear();
        groupUpdateTime.clear();
    }

    @Test
    void testInit() {
        clusterWatcherManager.init();
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor)
                ReflectionTestUtils.getField(clusterWatcherManager, "scheduledThreadPoolExecutor");

        assertNotNull(scheduledThreadPoolExecutor);
        assertFalse(scheduledThreadPoolExecutor.isShutdown());
    }

    @Test
    void testRegistryNewWatcher() {
        Watcher<AsyncContext> watcher =
                new Watcher<>(testGroup, asyncContext, testTimeout, testTerm, testClientEndpoint);
        clusterWatcherManager.registryWatcher(watcher);

        Map<String, Queue<Watcher<?>>> watchers =
                (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");

        assertNotNull(watchers);
        assertTrue(watchers.containsKey(testGroup));
        assertEquals(1, watchers.get(testGroup).size());
        assertFalse(watcher.isDone());
    }

    @Test
    void testRegistryWatcherOldTerm() {
        Map<String, Long> groupUpdateTime =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");
        groupUpdateTime.put(testGroup, testTerm + 10);

        Watcher<AsyncContext> watcher =
                new Watcher<>(testGroup, asyncContext, testTimeout, testTerm, testClientEndpoint);
        clusterWatcherManager.registryWatcher(watcher);

        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(asyncContext).complete();
        assertTrue(watcher.isDone());

        Map<String, Queue<Watcher<?>>> watchers =
                (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");

        assertFalse(watchers.containsKey(testGroup));
        assertNull(watchers.get(testGroup));
    }

    @Test
    void testOnEventChange() {
        Watcher<AsyncContext> watcher =
                new Watcher<>(testGroup, asyncContext, testTimeout, testTerm, testClientEndpoint);
        clusterWatcherManager.registryWatcher(watcher);
        Map<String, Queue<Watcher<?>>> watchers =
                (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");
        Map<String, Long> updateTime =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");

        assertNotNull(watchers);
        assertNotNull(updateTime);

        ClusterChangeEvent zeroTermEvent = new ClusterChangeEvent(this, testGroup, testNamespace, testCluster, 0);
        clusterWatcherManager.onChangeEvent(zeroTermEvent);

        assertEquals(0, updateTime.size());
        assertFalse(watcher.isDone());
        assertTrue(watchers.containsKey(testGroup));
        assertNotNull(watchers.get(testGroup));
        assertEquals(1, watchers.get(testGroup).size());

        ClusterChangeEvent event = new ClusterChangeEvent(this, testGroup, testNamespace, testCluster, testTerm + 1);
        clusterWatcherManager.onChangeEvent(event);

        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(asyncContext).complete();

        assertEquals(1, updateTime.size());
        assertEquals(testTerm + 1, updateTime.get(testGroup));
        assertTrue(watcher.isDone());
        assertFalse(watchers.containsKey(testGroup));
        assertNull(watchers.get(testGroup));
    }

    @Test
    void testGetWatcherIpList() {
        Watcher<AsyncContext> watcher1 = new Watcher<>(testGroup, asyncContext, testTimeout, testTerm, "127.0.0.1");
        Watcher<AsyncContext> watcher2 = new Watcher<>(testGroup, asyncContext, testTimeout, testTerm, "127.0.0.1");
        Watcher<AsyncContext> watcher3 = new Watcher<>(testGroup, asyncContext, testTimeout, testTerm, "192.168.1.1");

        clusterWatcherManager.registryWatcher(watcher1);
        clusterWatcherManager.registryWatcher(watcher2);
        clusterWatcherManager.registryWatcher(watcher3);
        List<String> watcherIpList = clusterWatcherManager.getWatcherIpList(testGroup);

        assertNotNull(watcherIpList);
        assertEquals(2, watcherIpList.size());
        assertTrue(watcherIpList.contains("127.0.0.1"));
        assertTrue(watcherIpList.contains("192.168.1.1"));
    }

    @Test
    void testGetWatchVGroupList() {
        Watcher<AsyncContext> watcher1 = new Watcher<>("VGroup1", asyncContext, testTimeout, testTerm, "127.0.0.1");
        Watcher<AsyncContext> watcher2 = new Watcher<>("VGroup1", asyncContext, testTimeout, testTerm, "127.0.0.2");
        Watcher<AsyncContext> watcher3 = new Watcher<>("VGroup2", asyncContext, testTimeout, testTerm, "192.168.1.1");

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
        Map<String, Long> groupUpdateTime =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");

        assertNotNull(groupUpdateTime);

        groupUpdateTime.put(testGroup, testTerm);
        Long term1 = clusterWatcherManager.getTermByvGroup(testGroup);
        Long term2 = clusterWatcherManager.getTermByvGroup("NotExist");

        assertEquals(testTerm, term1);
        assertEquals(0L, term2);
    }

    @Test
    void testNotifyWithNotModifiedStatus() {
        Watcher<AsyncContext> watcher =
                new Watcher<>(testGroup, asyncContext, testTimeout, testTerm, testClientEndpoint);

        ReflectionTestUtils.invokeMethod(clusterWatcherManager, "notify", watcher, HttpStatus.NOT_MODIFIED.value());

        Mockito.verify(response).setStatus(HttpStatus.NOT_MODIFIED.value());
        Mockito.verify(asyncContext).complete();
        assertTrue(watcher.isDone());
    }

    @Test
    void testScheduledTaskReRegisterNonTimeoutWatcher() throws InterruptedException {
        int timeoutDuration = 3000;
        Watcher<AsyncContext> watcher =
                new Watcher<>(testGroup, asyncContext, timeoutDuration, testTerm, testClientEndpoint);
        clusterWatcherManager.registryWatcher(watcher);

        clusterWatcherManager.init();
        TimeUnit.SECONDS.sleep(2);

        Mockito.verify(response, Mockito.never()).setStatus(Mockito.anyInt());
        Mockito.verify(asyncContext, Mockito.never()).complete();
        assertFalse(watcher.isDone());
        Map<String, Queue<Watcher<?>>> watchers =
                (Map<String, Queue<Watcher<?>>>) ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");
        assertTrue(watchers.containsKey(testGroup));
        assertEquals(1, watchers.get(testGroup).size());
    }

    @Test
    void testOnChangeEventWithTermMinus1() {
        Watcher<AsyncContext> watcher =
                new Watcher<>(testGroup, asyncContext, testTimeout, testTerm, testClientEndpoint);
        clusterWatcherManager.registryWatcher(watcher);

        ClusterChangeEvent minus1TermEvent = new ClusterChangeEvent(this, testGroup, testNamespace, testCluster, -1);
        clusterWatcherManager.onChangeEvent(minus1TermEvent);

        Map<String, Long> updateTime =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");
        assertEquals(-1L, updateTime.get(testGroup));
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(asyncContext).complete();
        assertTrue(watcher.isDone());
    }
}
