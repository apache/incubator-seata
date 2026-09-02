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
package org.apache.seata.server;

import com.alibaba.nacos.api.config.remote.request.*;
import com.alibaba.nacos.api.config.remote.response.*;
import com.alibaba.nacos.api.naming.remote.request.*;
import com.alibaba.nacos.api.naming.remote.response.*;
import com.alibaba.nacos.api.remote.request.*;
import com.alibaba.nacos.api.remote.response.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Pre-populates the Nacos {@code PayloadRegistry} with all known
 * {@code Request}/{@code Response} subclasses at application startup,
 * before any Nacos client beans are created.
 *
 * <p>The Nacos client (version 2.0.4) uses the Reflections library to
 * classpath-scan for {@code Payload} implementations in its
 * {@code PayloadRegistry.scan()} static initializer. In a GraalVM native
 * image, Reflections classpath scanning does not work, leaving the
 * registry empty. This causes {@code Unknown payload type} errors when
 * the Nacos gRPC client tries to parse server responses such as
 * {@code ServerCheckResponse}.
 *
 * <p>This initializer implements {@link PriorityOrdered} and must run
 * before any other initializer (e.g. {@code SeataPropertiesLoader}) that
 * may trigger class loading of {@code StoreConfig}, whose static
 * initializer instantiates the Nacos config client, which in turn
 * triggers {@code PayloadRegistry.init()} via {@code RpcClient.<clinit>}.
 *
 * @see com.alibaba.nacos.api.remote.PayloadRegistry
 */
public class NacosPayloadRegistryInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, PriorityOrdered {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosPayloadRegistryInitializer.class);

    /**
     * Fully-qualified class name of the Nacos PayloadRegistry.
     */
    private static final String PAYLOAD_REGISTRY_CLASS = "com.alibaba.nacos.api.remote.PayloadRegistry";

    /**
     * All known concrete {@code Request} subclasses in the
     * nacos-client 2.0.x classpath. Abstract classes like
     * {@code InternalRequest}, {@code ServerRequest},
     * {@code AbstractConfigRequest}, and {@code AbstractNamingRequest}
     * are intentionally omitted — PayloadRegistry.register() skips
     * them anyway.
     */
    private static final String[] REQUEST_CLASSES = {
        ClientConfigMetricRequest.class.getName(),
        ConfigBatchListenRequest.class.getName(),
        ConfigChangeNotifyRequest.class.getName(),
        ConfigPublishRequest.class.getName(),
        ConfigQueryRequest.class.getName(),
        ConfigRemoveRequest.class.getName(),
        InstanceRequest.class.getName(),
        NotifySubscriberRequest.class.getName(),
        ServiceListRequest.class.getName(),
        ServiceQueryRequest.class.getName(),
        SubscribeServiceRequest.class.getName(),
        ClientDetectionRequest.class.getName(),
        ConnectResetRequest.class.getName(),
        ConnectionSetupRequest.class.getName(),
        HealthCheckRequest.class.getName(),
        PushAckRequest.class.getName(),
        ServerCheckRequest.class.getName(),
        ServerLoaderInfoRequest.class.getName(),
        ServerReloadRequest.class.getName(),
    };

    /**
     * All known concrete {@code Response} subclasses in the
     * nacos-client 2.0.x classpath.
     */
    private static final String[] RESPONSE_CLASSES = {
        ClientConfigMetricResponse.class.getName(),
        ConfigChangeBatchListenResponse.class.getName(),
        ConfigChangeNotifyResponse.class.getName(),
        ConfigPublishResponse.class.getName(),
        ConfigQueryResponse.class.getName(),
        ConfigRemoveResponse.class.getName(),
        InstanceResponse.class.getName(),
        NotifySubscriberResponse.class.getName(),
        QueryServiceResponse.class.getName(),
        ServiceListResponse.class.getName(),
        SubscribeServiceResponse.class.getName(),
        ClientDetectionResponse.class.getName(),
        ConnectResetResponse.class.getName(),
        ErrorResponse.class.getName(),
        HealthCheckResponse.class.getName(),
        ServerCheckResponse.class.getName(),
        ServerLoaderInfoResponse.class.getName(),
        ServerReloadResponse.class.getName(),
    };

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        try {
            populatePayloadRegistry();
        } catch (Exception e) {
            LOGGER.warn(
                    "Failed to pre-populate Nacos PayloadRegistry. "
                            + "Nacos gRPC communication may fail with "
                            + "'Unknown payload type' errors.",
                    e);
        }
    }

    private void populatePayloadRegistry() throws Exception {
        Class<?> payloadRegistryClass = Class.forName(PAYLOAD_REGISTRY_CLASS);

        // Access the private static REGISTRY_REQUEST map
        Field registryField = payloadRegistryClass.getDeclaredField("REGISTRY_REQUEST");
        registryField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Class<?>> registryMap = (Map<String, Class<?>>) registryField.get(null);

        // Access the initialized flag so we can prevent the broken
        // Reflections-based scan from running later
        Field initializedField = payloadRegistryClass.getDeclaredField("initialized");
        initializedField.setAccessible(true);

        int count = 0;

        // Register all Request subclasses
        for (String className : REQUEST_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                String simpleName = clazz.getSimpleName();
                if (!registryMap.containsKey(simpleName)) {
                    registryMap.put(simpleName, clazz);
                    count++;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Nacos request class not on classpath, skipping: {}", className);
            }
        }

        // Register all Response subclasses
        for (String className : RESPONSE_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                String simpleName = clazz.getSimpleName();
                if (!registryMap.containsKey(simpleName)) {
                    registryMap.put(simpleName, clazz);
                    count++;
                }
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Nacos response class not on classpath, skipping: {}", className);
            }
        }

        // Mark as initialized to prevent the Reflections-based scan
        // from running (and potentially failing or throwing duplicate
        // key exceptions)
        initializedField.setBoolean(null, true);

        LOGGER.info(
                "Pre-populated Nacos PayloadRegistry with {} request/response types "
                        + "(Reflections classpath scanning unavailable in native image)",
                count);
    }
}
