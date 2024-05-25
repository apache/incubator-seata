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
package org.apache.seata.benchmark.profiler.base;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.agent.attach.ArthasAgent;
import com.taobao.arthas.core.server.ArthasBootstrap;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncProfiler implements Profiler {

    public static final Logger LOGGER = LoggerFactory.getLogger(AsyncProfiler.class);

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            Map<String, String> configMap = new HashMap<String, String>();
            ArthasAgent.attach(configMap);
        }
    }

    @Override
    public void profile(Runnable runnable, EventType eventType) throws Exception {
        try {
            ArthasCommand startProfilerCommand = new ArthasCommand();
            startProfilerCommand.setAction("exec");
            startProfilerCommand.setCommand("profiler start --event " + eventType.name());

            String startResponse = postCommand(startProfilerCommand);
            LOGGER.info("start profile: " + startResponse);

            runnable.run();
        } finally {
            ArthasCommand stopProfilerCommand = new ArthasCommand();
            stopProfilerCommand.setAction("exec");
            stopProfilerCommand.setCommand("profiler stop --format html");

            String stopResponse = postCommand(stopProfilerCommand);
            LOGGER.info("stop profile: " + stopResponse);
        }
    }

    @Override
    public void profile(Runnable runnable, EventType eventType, int warmUpIterations, int profileIterations) throws Exception {
        for (int i = 0; i < warmUpIterations; i++) {
            LOGGER.info("warmUp iteration: " + i);
            runnable.run();
        }

        try {
            ArthasCommand startProfilerCommand = new ArthasCommand();
            startProfilerCommand.setAction("exec");
            startProfilerCommand.setCommand("profiler start --event " + eventType.name());

            String startResponse = postCommand(startProfilerCommand);
            LOGGER.info("start profile: " + startResponse);

            for (int i = 0; i < profileIterations; i++) {
                runnable.run();
            }
        } finally {
            ArthasCommand stopProfilerCommand = new ArthasCommand();
            stopProfilerCommand.setAction("exec");
            stopProfilerCommand.setCommand("profiler stop --format html");

            String stopResponse = postCommand(stopProfilerCommand);
            LOGGER.info("stop profile: " + stopResponse);

            LOGGER.info("profile report: http://localhost:3658/arthas-output/");
        }
    }

    private String postCommand(ArthasCommand command) throws IOException {
        HttpPost httpPost = new HttpPost("http://localhost:8563/api");

        StringEntity entity = new StringEntity(JSON.toJSONString(command));
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        CloseableHttpResponse response = httpClient.execute(httpPost);
        return EntityUtils.toString(response.getEntity());
    }

    @Override
    public void destroy() {
        ArthasBootstrap.getInstance().destroy();
    }


    public static class ArthasCommand {
        private String action;
        private String command;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }
}
