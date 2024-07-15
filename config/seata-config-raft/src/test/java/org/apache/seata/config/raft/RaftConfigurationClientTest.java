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
package org.apache.seata.config.raft;


import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.seata.common.util.HttpClientUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RaftConfigurationClientTest {
    @Test
    public void testRaftConfigurationClient() {
// 创建一个后台线程
        Thread backgroundThread = new Thread(() -> RaftConfigurationClient.getInstance());

        // 将线程设置为守护线程
        backgroundThread.setDaemon(true);
        backgroundThread.start();

        // 主线程保持运行
        try {
            System.out.println("主线程正在运行...");
            Thread.sleep(60000); // 主线程睡眠60秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("主线程结束");

    }
}
