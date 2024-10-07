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
package org.apache.seata.core.rpc.netty.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpDispatchHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpDispatchHandler.class);

    private static final String INVALID_DEFAULT_VALUE = "\"\\n\\t\\t\\n\\t\\t\\n\\ue000\\ue001\\ue002\\n\\t\\t\\t\\t\\n";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest httpRequest) throws Exception {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());
        String path = queryStringDecoder.path();
        JSONObject paramMap = new JSONObject();
        if (httpRequest.method() == HttpMethod.GET) {
            paramMap.putAll(convertParamMap(queryStringDecoder.parameters()));
        } else if (httpRequest.method() == HttpMethod.POST) {
            HttpPostRequestDecoder httpPostRequestDecoder = new HttpPostRequestDecoder(httpRequest);
            for (InterfaceHttpData interfaceHttpData : httpPostRequestDecoder.getBodyHttpDatas()) {
                if (interfaceHttpData.getHttpDataType() != InterfaceHttpData.HttpDataType.Attribute) {
                    continue;
                }
                Attribute attribute = (Attribute) interfaceHttpData;
                paramMap.put(attribute.getName(), attribute.getValue());
            }
        }

        SeataHttpServletRequest seataHttpServletRequest = new SeataHttpServletRequest(ctx.channel().remoteAddress().toString());
        Object httpController = ControllerManager.getHttpController(path);
        Method handleMethod = ControllerManager.getHandleMethod(path);
        Class<?>[] parameterTypes = handleMethod.getParameterTypes();
        Annotation[][] parameterAnnotations = handleMethod.getParameterAnnotations();
        Parameter[] parameters = handleMethod.getParameters();
        Object[] args = getParameters(seataHttpServletRequest, parameterTypes, parameterAnnotations, parameters, paramMap);
        Object result = handleMethod.invoke(httpController, args);
        if (seataHttpServletRequest.isAsyncStarted()) {
            seataHttpServletRequest.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                    ctx.channel().writeAndFlush(((SeataHttpServletResponse) event.getAsyncContext().getResponse()).getHttpResponse());
                }

                @Override
                public void onTimeout(AsyncEvent event) throws IOException {

                }

                @Override
                public void onError(AsyncEvent event) throws IOException {

                }

                @Override
                public void onStartAsync(AsyncEvent event) throws IOException {

                }
            });

            return;
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(JSON.toJSONBytes(result)));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    private Object[] getParameters(SeataHttpServletRequest seataHttpServletRequest, Class<?>[] parameterTypes, Annotation[][] parameterAnnotations, Parameter[] parameters, JSONObject paramMap) {
        int length = parameterTypes.length;
        Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            Class<?> parameterType = parameterTypes[i];
            String parameterName = parameters[i].getName();
            Class<? extends Annotation> parameterAnnotationType = null;
            Annotation parameterAnnotation = null;
            if (parameterAnnotations[i] != null && parameterAnnotations[i].length > 0) {
                parameterAnnotationType = parameterAnnotations[i][0].annotationType();
                parameterAnnotation = parameterAnnotations[i][0];
            }

            if (parameterAnnotationType == null) {
                parameterAnnotationType = RequestParam.class;
            }

            ret[i] = getArgValue(seataHttpServletRequest, parameterType, parameterName, parameterAnnotationType, parameterAnnotation, paramMap);
            if (!parameterType.isAssignableFrom(ret[i].getClass())) {
                LOGGER.error("[HttpDispatchHandler] not compatible parameter type, expect {}, but {}", parameterType, ret[i].getClass());
                ret[i] = null;
            }
        }

        return ret;
    }

    private Object getArgValue(SeataHttpServletRequest seataHttpServletRequest, Class<?> parameterType, String parameterName, Class<? extends Annotation> parameterAnnotationType, Annotation parameterAnnotation, JSONObject paramMap) {
        if (parameterType.equals(HttpServletRequest.class)) {
            return seataHttpServletRequest;
        } else if (parameterAnnotationType.equals(ModelAttribute.class)) {
            return paramMap.toJavaObject(parameterType);
        } else if (parameterAnnotationType.equals(RequestBody.class)) {
            return JSONObject.parseObject(paramMap.getString(parameterName));
        } else {
            String paramValue = paramMap.getString(parameterName);
            return paramValue == null ? getDefaultValue(parameterAnnotation) : paramValue;
        }
    }

    private static String getDefaultValue(Annotation parameterAnnotation) {
        if (!(parameterAnnotation instanceof RequestParam)) {
            return null;
        }

        RequestParam annotation = (RequestParam) parameterAnnotation;
        return annotation.defaultValue().equals(INVALID_DEFAULT_VALUE) ? null : annotation.defaultValue();
    }

    private static Map<String, String> convertParamMap(Map<String, List<String>> paramMap) {
        Map<String, String> ret = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : paramMap.entrySet()) {
            List<String> list = entry.getValue();
            if (list != null && !list.isEmpty()) {
                ret.put(entry.getKey(), list.get(0));
            }
        }
        return ret;
    }
}