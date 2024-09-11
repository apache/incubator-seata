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
package org.apache.seata.config.apollo;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * The type Apollo mock server.
 */
public class ApolloMockServer {

    private MockWebServer server;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String CONFIG_PREFIX_PATH = "/configs";

    /**
     * Instantiates a new Apollo mock server.
     *
     * @param port the port
     * @throws IOException the io exception
     */
    public ApolloMockServer(int port) throws IOException {

        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().startsWith(CONFIG_PREFIX_PATH)) {
                    List<String> pathSegments = request.getRequestUrl().pathSegments();
                    String appId = pathSegments.get(1);
                    String cluster = pathSegments.get(2);
                    String namespace = pathSegments.get(3);
                    String result;
                    try {
                        result = loadMockData(appId, cluster, namespace);
                        return new MockResponse().setResponseCode(200).setBody(result);
                    } catch (JsonProcessingException e) {
                    }
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        server.start(port);
        System.setProperty("apollo.configService", "http://localhost:" + port);

    }

    private String loadMockData(String appId, String Cluster, String namespace) throws JsonProcessingException {
        String fileName = "mock-" + namespace + ".properties";
        ApolloConfig apolloConfig = new ApolloConfig(appId, Cluster, namespace, "releaseKey");
        Properties properties = new Properties();
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (null != input) {
                properties.load(input);
            }
        } catch (Exception ignore) {
        }
        Map<String, String> configurations = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            configurations.put(entry.getKey().toString(), entry.getValue().toString());
        }
        apolloConfig.setConfigurations(configurations);
        String json = mapper.writeValueAsString(apolloConfig);
        return json;

    }

    /**
     * Stop.
     *
     * @throws IOException the io exception
     */
    public void stop() throws IOException {
        if (null != server) {
            server.shutdown();
        }
    }

}
