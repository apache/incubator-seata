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
package org.apache.seata.server.metrics;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.event.EventBus;
import org.apache.seata.core.event.ExceptionEvent;
import org.apache.seata.core.event.GlobalTransactionEvent;
import org.apache.seata.core.event.RateLimitEvent;
import org.apache.seata.core.rpc.Disposable;
import org.apache.seata.metrics.Summary;
import org.apache.seata.metrics.exporter.Exporter;
import org.apache.seata.metrics.exporter.ExporterFactory;
import org.apache.seata.metrics.registry.Registry;
import org.apache.seata.metrics.registry.RegistryFactory;
import org.apache.seata.server.ServerRunner;
import org.apache.seata.server.event.EventBusManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_ENABLED;
import static org.apache.seata.common.DefaultValues.DEFAULT_METRICS_EXPORTER_LIST;
import static org.apache.seata.common.DefaultValues.DEFAULT_PROMETHEUS_PORT;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_ENABLED;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_EXPORTER_LIST;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_EXPORTER_PROMETHEUS_PORT;
import static org.apache.seata.core.constants.ConfigurationKeys.METRICS_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MetricsManagerTest {
    private boolean installedEnvironment;

    @BeforeEach
    void provideConfigurationEnvironment() {
        installedEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT) == null;
        if (installedEnvironment) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new StandardEnvironment());
        }
    }

    @AfterEach
    void restoreConfigurationEnvironment() {
        if (installedEnvironment) {
            Map<?, ?> objects = (Map<?, ?>) ReflectionTestUtils.getField(ObjectHolder.class, "OBJECT_MAP");
            objects.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void retriesPrometheusAndReleasesItsPortOnServerShutdown() throws Exception {
        // Preserve resources owned by any running Spring test context in this test JVM.
        Map<Class<?>, Object> spiLoaders =
                (Map<Class<?>, Object>) ReflectionTestUtils.getField(EnhancedServiceLoader.class, "SERVICE_LOADERS");
        List<Disposable> disposables =
                (List<Disposable>) ReflectionTestUtils.getField(ServerRunner.class, "DISPOSABLE_LIST");
        List<Disposable> previousDisposables = new ArrayList<>(disposables);
        Object previousExporterLoader = spiLoaders.remove(Exporter.class);
        disposables.clear();
        MetricsManager manager = new MetricsManager();
        ServerRunner runner = new ServerRunner();
        Configuration config = mock(Configuration.class);
        Registry registry = mock(Registry.class);
        Summary summary = mock(Summary.class);
        EventBus eventBus = mock(EventBus.class);
        when(config.getBoolean(METRICS_PREFIX + METRICS_ENABLED, DEFAULT_METRICS_ENABLED))
                .thenReturn(true);
        when(config.getConfig(METRICS_PREFIX + METRICS_EXPORTER_LIST, DEFAULT_METRICS_EXPORTER_LIST))
                .thenReturn("prometheus");
        when(registry.getSummary(any())).thenReturn(summary);

        try (MockedStatic<ConfigurationFactory> configurations = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registries = mockStatic(RegistryFactory.class);
                MockedStatic<EventBusManager> eventBuses = mockStatic(EventBusManager.class);
                MockedStatic<MetricsManager> managers = mockStatic(MetricsManager.class)) {
            configurations.when(ConfigurationFactory::getInstance).thenReturn(config);
            registries.when(RegistryFactory::getInstance).thenReturn(registry);
            eventBuses.when(EventBusManager::get).thenReturn(eventBus);
            managers.when(MetricsManager::get).thenReturn(manager);

            int port;
            try (ServerSocket occupied = new ServerSocket(0)) {
                port = occupied.getLocalPort();
                when(config.getInt(METRICS_PREFIX + METRICS_EXPORTER_PROMETHEUS_PORT, DEFAULT_PROMETHEUS_PORT))
                        .thenReturn(port);
                manager.init();
                verifyNoInteractions(eventBus);
            }

            MetricsSubscriber retired = startAndStopMetrics(manager, runner, eventBus, registry, port);
            doThrow(new IllegalStateException("injected unregister failure"))
                    .when(eventBus)
                    .unregister(any(MetricsSubscriber.class));
            MetricsSubscriber next = startAndStopMetrics(manager, runner, eventBus, registry, port);
            assertNotSame(retired, next);
        } finally {
            spiLoaders.remove(Exporter.class);
            if (previousExporterLoader != null) {
                spiLoaders.put(Exporter.class, previousExporterLoader);
            }
            disposables.clear();
            disposables.addAll(previousDisposables);
        }
    }

    @Test
    void skipsExportersWhenMetricsAreDisabledOrRegistryIsMissing() {
        MetricsManager manager = new MetricsManager();
        Configuration config = mock(Configuration.class);
        when(config.getBoolean(METRICS_PREFIX + METRICS_ENABLED, DEFAULT_METRICS_ENABLED))
                .thenReturn(false, true);
        try (MockedStatic<ConfigurationFactory> configurations = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registries = mockStatic(RegistryFactory.class);
                MockedStatic<ExporterFactory> factories = mockStatic(ExporterFactory.class)) {
            configurations.when(ConfigurationFactory::getInstance).thenReturn(config);
            registries.when(RegistryFactory::getInstance).thenReturn(null);

            manager.init();
            registries.verifyNoInteractions();
            factories.verifyNoInteractions();

            manager.init();
            registries.verify(RegistryFactory::getInstance);
            factories.verifyNoInteractions();
            assertNull(manager.getRegistry());
        }
    }

    @Test
    void retriesAfterRegistrationFailsEvenWhenRollbackEncountersErrors() throws IOException {
        MetricsManager manager = new MetricsManager();
        Configuration config = mock(Configuration.class);
        Registry registry = mock(Registry.class);
        EventBus eventBus = mock(EventBus.class);
        Exporter failingExporter = mock(Exporter.class);
        Exporter otherExporter = mock(Exporter.class);
        Exporter replacementExporter = mock(Exporter.class);
        IllegalStateException registrationFailure = new IllegalStateException("injected registration failure");

        when(config.getBoolean(METRICS_PREFIX + METRICS_ENABLED, DEFAULT_METRICS_ENABLED))
                .thenReturn(true);
        doThrow(registrationFailure).doNothing().when(eventBus).register(any(MetricsSubscriber.class));
        doThrow(new IOException("injected exporter close failure"))
                .when(failingExporter)
                .close();
        doThrow(new IllegalStateException("injected registry cleanup failure"))
                .doNothing()
                .when(registry)
                .clearUp();

        try (MockedStatic<ConfigurationFactory> configurations = mockStatic(ConfigurationFactory.class);
                MockedStatic<RegistryFactory> registries = mockStatic(RegistryFactory.class);
                MockedStatic<ExporterFactory> factories = mockStatic(ExporterFactory.class);
                MockedStatic<EventBusManager> eventBuses = mockStatic(EventBusManager.class);
                MockedStatic<EnhancedServiceLoader> loaders = mockStatic(EnhancedServiceLoader.class)) {
            configurations.when(ConfigurationFactory::getInstance).thenReturn(config);
            registries.when(RegistryFactory::getInstance).thenReturn(registry);
            factories
                    .when(ExporterFactory::getInstanceList)
                    .thenReturn(List.of(failingExporter, otherExporter), List.of(replacementExporter));
            eventBuses.when(EventBusManager::get).thenReturn(eventBus);

            try {
                assertSame(registrationFailure, assertThrows(IllegalStateException.class, manager::init));
                ArgumentCaptor<MetricsSubscriber> subscriber = ArgumentCaptor.forClass(MetricsSubscriber.class);
                verify(eventBus).register(subscriber.capture());
                verify(eventBus).unregister(same(subscriber.getValue()));
                verify(failingExporter).close();
                verify(otherExporter).close();
                loaders.verify(() -> EnhancedServiceLoader.unload(Exporter.class));
                verify(registry).clearUp();
                assertNull(manager.getRegistry());

                manager.init();
                verify(replacementExporter).setRegistry(same(registry));
                assertSame(registry, manager.getRegistry());
            } finally {
                manager.destroy();
            }
            verify(replacementExporter).close();
        }
    }

    private static MetricsSubscriber startAndStopMetrics(
            MetricsManager manager, ServerRunner runner, EventBus eventBus, Registry registry, int port)
            throws Exception {
        try {
            manager.init();
            assertThrows(BindException.class, () -> new ServerSocket(port).close());
            manager.init();
            ArgumentCaptor<MetricsSubscriber> subscriber = ArgumentCaptor.forClass(MetricsSubscriber.class);
            verify(eventBus).register(subscriber.capture());
            MetricsSubscriber retired = subscriber.getValue();
            runner.destroy();
            try (ServerSocket released = new ServerSocket(port)) {
                assertEquals(port, released.getLocalPort());
            }
            runner.destroy();
            verify(eventBus).unregister(same(retired));
            verify(registry).clearUp();
            assertNull(manager.getRegistry());

            clearInvocations(eventBus, registry);
            // Model callbacks that were submitted before unregistration and run after shutdown.
            retired.exceptionEventForMetrics(new ExceptionEvent("queued"));
            retired.recordRateLimitEventForMetrics(new RateLimitEvent("trace", "token", "app", "host:8091"));
            retired.recordGlobalTransactionEventForMetrics(
                    new GlobalTransactionEvent(1, "tc", "order", "app", "group", 10L, 40L, "Begin", false, false));
            verifyNoInteractions(registry);
            return retired;
        } finally {
            // Ensure failed lifecycle assertions cannot leave a listener running in this test JVM.
            manager.destroy();
        }
    }
}
