package org.apache.seata.core.rpc.netty.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpDispatchHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Map<String, HttpController> httpControllerMap = new ConcurrentHashMap<>();

    public static HttpController getHttpController(String path) {
        return httpControllerMap.get(path);
    }

    public static void addHttpController(HttpController httpController) {
        for (String path : httpController.getPath()) {
            httpControllerMap.put(path, httpController);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest httpRequest) throws Exception {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());
        String path = queryStringDecoder.path();
        Map<String, List<String>> paramMap = new HashMap<>();
        if (httpRequest.method() == HttpMethod.GET) {
            paramMap.putAll(queryStringDecoder.parameters());
        } else if (httpRequest.method() == HttpMethod.POST) {
            HttpPostRequestDecoder httpPostRequestDecoder = new HttpPostRequestDecoder(httpRequest);
            for (InterfaceHttpData interfaceHttpData : httpPostRequestDecoder.getBodyHttpDatas()) {
                if (interfaceHttpData.getHttpDataType() != InterfaceHttpData.HttpDataType.Attribute) {
                    continue;
                }
                Attribute attribute = (Attribute) interfaceHttpData;
                paramMap.put(attribute.getName(), new ArrayList<String>() {{
                    add(attribute.getValue());
                }});
            }

            String result = getHttpController(path).handle(path, paramMap);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(result.getBytes()));
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response);
        }
    }
}