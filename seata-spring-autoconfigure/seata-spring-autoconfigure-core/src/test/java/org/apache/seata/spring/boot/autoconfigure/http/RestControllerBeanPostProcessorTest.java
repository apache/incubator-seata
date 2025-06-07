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
package org.apache.seata.spring.boot.autoconfigure.http;

import org.apache.seata.core.rpc.netty.http.ControllerManager;
import org.apache.seata.core.rpc.netty.http.HttpInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.*;

import static org.mockito.Mockito.verify;

public class RestControllerBeanPostProcessorTest {

    @Mock
    private ControllerManager controllerManager;

    private RestControllerBeanPostProcessor processor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new RestControllerBeanPostProcessor();
    }

    @Test
    public void testPostProcessAfterInitialization() throws Exception {
        // Mock the bean and its annotations
        TestController mockBean = new TestController();

        // Call the method under test
        processor.postProcessAfterInitialization(mockBean, "testController");

        // Verify that the paths were added correctly
        HttpInvocation httpInvocation = new HttpInvocation();
        httpInvocation.setPath("/path");
        verify(controllerManager).addHttpInvocation(httpInvocation);
    }

    @RestController
    @RequestMapping("/base")
    static class TestController {

        @GetMapping("/get")
        public String getMethod(@RequestParam String param) {
            return "GET";
        }

        @PostMapping("/post")
        public String postMethod(@RequestBody String body) {
            return "POST";
        }
    }
}



