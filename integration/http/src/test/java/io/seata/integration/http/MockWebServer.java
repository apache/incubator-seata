/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.integration.http;

import com.alibaba.fastjson.JSONObject;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static io.seata.integration.http.AbstractHttpExecutor.convertParamOfJsonString;

/**
 * @author : wangxb
 */
public class MockWebServer {

    private Map<String, String> urlServletMap = new HashMap<>();


    public void start(int port) {
        initServletMapping();
        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);
                Socket socket = serverSocket.accept();
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                MockRequest myRequest = new MockRequest(inputStream);
                MockResponse myResponse = new MockResponse(outputStream);

                dispatch(myRequest, myResponse);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    public void initServletMapping() {
        for (ServletMapping servletMapping : ServletMapping.servletMappingList) {
            urlServletMap.put(servletMapping.getPath(), servletMapping.getClazz() + "_" + servletMapping.getMethod());
        }
    }

    public String dispatch(MockRequest myRequest, MockResponse mockResponse) {
        String clazz = urlServletMap.get(myRequest.getPath()).split("_")[0];
        String methodName = urlServletMap.get(myRequest.getPath()).split("_")[1];
        HttpServletRequest request = new MockHttpServletRequest(myRequest);
        try {
            Class<MockController> myServletClass = (Class<MockController>) Class.forName(clazz);
            MockController myServlet = myServletClass.newInstance();
            HttpTest.Person person = boxing(myRequest);
            Method method = myServletClass.getDeclaredMethod(methodName, HttpTest.Person.class);

            /* mock request intercepter */
            TransactionPropagationInterceptor intercepter = new TransactionPropagationInterceptor();

            intercepter.preHandle(request, null, null);
            Object result = method.invoke(myServlet, person);

            return mockResponse.write(result.toString());
        } catch (Exception e) {
            HttpHandlerExceptionResolver resolver = new HttpHandlerExceptionResolver();
            resolver.doResolveException(request, null, null, e);
            if (RootContext.getXID() == null) {
                try {
                    return mockResponse.write("Callee remove local xid success");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException(e);
        }
    }

    private HttpTest.Person boxing(MockRequest myRequest) {
        Map params = null;
        if ("get".equals(myRequest.getMethod()))
            params = getUrlParams(myRequest.getUrl());
        else if ("post".equals(myRequest.getMethod())) {
            params = getBodyParams(myRequest.getBody());
        }
        return JSONObject.parseObject(JSONObject.toJSONString(params), HttpTest.Person.class);
    }

    private Map<String, String> getBodyParams(String body) {
        Map<String, String> map = convertParamOfJsonString(body, HttpTest.Person.class);
        return map;
    }


    public static Map<String, Object> getUrlParams(String param) {
        Map<String, Object> map = new HashMap<String, Object>(0);
        if (StringUtils.isBlank(param)) {
            return map;
        }
        String[] urlPath = param.split("\\?");
        if (urlPath.length < 2) {
            return map;
        }

        String[] params = urlPath[1].split("&");
        for (int i = 0; i < params.length; i++) {
            String[] p = params[i].split("=");
            if (p.length == 2) {
                map.put(p[0], p[1]);
            }
        }
        return map;

    }
}