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
import org.apache.seata.core.rpc.netty.http.ParamMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    public void testRegisterHttpInvocationWithCorrectMetadata() {
        // Mock the bean and its annotations
        TestApiController controller = new TestApiController();

        // Call the method under test
        processor.postProcessAfterInitialization(controller, "testApiController");

        // Verify whether the parsed data is correct
        HttpInvocation getInvocation = ControllerManager.getHttpInvocation("/api/get");
        assertNotNull(getInvocation, "getMethod should be registered");
        assertEquals("getMethod", getInvocation.getMethod().getName());
        assertSame(controller, getInvocation.getController());

        // Verify whether the defaultValue attribute "value" of @RequestParam is correct
        ParamMetaData[] getParams = getInvocation.getParamMetaData();
        assertEquals(1, getParams.length);
        assertEquals("param", getParams[0].getParamName());
        assertEquals("defaultValue", getParams[0].getDefaultValue());
        assertEquals(ParamMetaData.ParamConvertType.REQUEST_PARAM, getParams[0].getParamConvertType());
        assertFalse(getParams[0].isRequired());

        // Verify whether the default value of the "required" attribute of @RequestParam is correct
        HttpInvocation postInvocation = ControllerManager.getHttpInvocation("/api/post");
        assertNotNull(postInvocation, "postMethod should be registered");
        assertEquals("postMethod", postInvocation.getMethod().getName());
        assertSame(controller, postInvocation.getController());

        // Verify whether the value of the "name" attribute of @RequestParam is correct
        ParamMetaData[] postParams = postInvocation.getParamMetaData();
        assertEquals(1, postParams.length);
        assertEquals("requestBody", postParams[0].getParamName());
        assertEquals(ParamMetaData.ParamConvertType.REQUEST_PARAM, postParams[0].getParamConvertType());

        HttpInvocation updateInvocation = ControllerManager.getHttpInvocation("/api/update");
        assertNotNull(updateInvocation, "updateMethod should be registered");
        assertEquals("updateMethod", updateInvocation.getMethod().getName());
        assertSame(controller, updateInvocation.getController());

        ParamMetaData[] updateParams = updateInvocation.getParamMetaData();
        assertEquals(2, updateParams.length);

        // Verify whether the value attribute of @RequestParam is correct
        assertEquals("userName", updateParams[0].getParamName());
        assertEquals(ParamMetaData.ParamConvertType.REQUEST_PARAM, updateParams[0].getParamConvertType());
        assertEquals("age", updateParams[1].getParamName());

        // Verify whether @RequestParam can be correctly parsed when there are multiple annotations before a parameter
        assertEquals(ParamMetaData.ParamConvertType.REQUEST_PARAM, updateParams[1].getParamConvertType());
        assertFalse(updateParams[1].isRequired());
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

    @RestController
    @RequestMapping("/api")
    static class TestApiController {

        @GetMapping("/get")
        public String getMethod(@RequestParam(defaultValue = "defaultValue") String param) {
            return "GET";
        }

        @PostMapping("/post")
        public String postMethod(@RequestParam(name = "requestBody") String body) {
            return "POST";
        }

        @GetMapping("/update")
        public String updateMethod(@RequestParam(value = "userName") String name,
                                   @Nonnull @RequestParam(required = false) Integer age) {
            return "update";
        }
    }
}