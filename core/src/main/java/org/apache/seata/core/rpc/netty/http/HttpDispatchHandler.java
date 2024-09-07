package org.apache.seata.core.rpc.netty.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.springframework.web.bind.annotation.*;

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
import java.util.concurrent.ConcurrentHashMap;

public class HttpDispatchHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Map<String, HttpController> httpControllerMap = new ConcurrentHashMap<>();
    private static final Map<String, Method> requestMethodMap = new ConcurrentHashMap<>();

    public static HttpController getHttpController(String path) {
        return httpControllerMap.get(path);
    }

    public static Method getHandleMethod(String path) {
        return requestMethodMap.get(path);
    }

    public static void addHttpController(HttpController httpController) {
        Class<? extends HttpController> httpControllerClass = httpController.getClass();
        RequestMapping requestMapping = httpControllerClass.getAnnotation(RequestMapping.class);
        String[] prePaths = requestMapping.value();
        Method[] methods = httpControllerClass.getMethods();
        for (Method method : methods) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                String[] postPaths = getMapping.value();
                for (String prePath : prePaths) {
                    for (String postPath : postPaths) {
                        requestMethodMap.put(prePath + "/" + postPath, method);
                        httpControllerMap.put(prePath + "/" + postPath, httpController);
                    }
                }
            }

            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                String[] postPaths = postMapping.value();
                for (String prePath : prePaths) {
                    for (String postPath : postPaths) {
                        requestMethodMap.put(prePath + "/" + postPath, method);
                        httpControllerMap.put(prePath + "/" + postPath, httpController);
                    }
                }
            }
        }

        for (String path : httpController.getPath()) {
            httpControllerMap.put(path, httpController);
        }
    }

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

        SeataHttpServletRequest seataHttpServletRequest = new SeataHttpServletRequest(httpRequest, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
        HttpController httpController = getHttpController(path);
        Method handleMethod = getHandleMethod(path);
        Class<?>[] parameterTypes = handleMethod.getParameterTypes();
        Annotation[][] parameterAnnotations = handleMethod.getParameterAnnotations();
        Parameter[] parameters = handleMethod.getParameters();
        Object[] args = getParameters(seataHttpServletRequest, parameterTypes, parameterAnnotations, parameters, paramMap);
        Object result = handleMethod.invoke(httpController, args);
        if (seataHttpServletRequest.isAsyncStarted()) {
            seataHttpServletRequest.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) throws IOException {
                    ctx.channel().writeAndFlush(((SeataHttpServletResponse)event.getAsyncContext().getResponse()).getHttpResponse());
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
            if (parameterAnnotations[i] != null && parameterAnnotations[i].length > 0) {
                parameterAnnotationType = parameterAnnotations[i][0].annotationType();
            }

            if (parameterAnnotationType == null) {
                parameterAnnotationType = RequestParam.class;
            }

            ret[i] = getArgValue(seataHttpServletRequest, parameterType, parameterName, parameterAnnotationType, paramMap);
        }

        return ret;
    }

    private Object getArgValue(SeataHttpServletRequest seataHttpServletRequest, Class<?> parameterType, String parameterName, Class<? extends Annotation> parameterAnnotationType, JSONObject paramMap) {
        if (parameterType.equals(HttpServletRequest.class)) {
            return seataHttpServletRequest;
        } else if (parameterAnnotationType.equals(ModelAttribute.class)) {
            return paramMap.toJavaObject(parameterType);
        } else if (parameterAnnotationType.equals(RequestBody.class)) {
            return JSONObject.parseObject(paramMap.getString(parameterName));
        } else {
            return paramMap.getString(parameterName);
        }
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