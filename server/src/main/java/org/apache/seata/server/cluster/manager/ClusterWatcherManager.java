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
package org.apache.seata.server.cluster.manager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.listener.ClusterChangeListener;
import org.apache.seata.server.cluster.watch.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ClusterWatcherManager implements ClusterChangeListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Map<String, Queue<Watcher<HttpContext>>> WATCHERS = new ConcurrentHashMap<>();

    private static final Map<String, Long> GROUP_UPDATE_TIME = new ConcurrentHashMap<>();

    private static final Map<Watcher<HttpContext>, Boolean> HTTP2_HEADERS_SENT = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("long-polling", 1));

    @PostConstruct
    public void init() {
        // Responds to monitors that time out
        scheduledThreadPoolExecutor.scheduleAtFixedRate(
                () -> {
                    for (String group : WATCHERS.keySet()) {
                        Optional.ofNullable(WATCHERS.remove(group))
                                .ifPresent(watchers -> watchers.parallelStream().forEach(watcher -> {
                                    if (System.currentTimeMillis() >= watcher.getTimeout()) {
                                        watcher.setDone(true);
                                        // 如果超时则一定关闭流，无论是http1还是http2
                                        // 对于HTTP/2：注册之前已经发送过headers frame，只能发送endStream=true的数据帧关闭流
                                        // 如果没有发送过headers frame，可以发送304的headers frame并关闭流
                                        boolean headersAlreadySent = HTTP2_HEADERS_SENT.getOrDefault(watcher, false);
                                        sendWatcherResponse(
                                                watcher, HttpResponseStatus.NOT_MODIFIED, true, !headersAlreadySent);
                                        HTTP2_HEADERS_SENT.remove(watcher);
                                    } else if (!watcher.isDone()) {
                                        // Re-register if not done and not timeout
                                        registryWatcher(watcher);
                                    }
                                }));
                    }
                },
                1,
                1,
                TimeUnit.SECONDS);
    }

    @Override
    @EventListener
    @Async
    public void onChangeEvent(ClusterChangeEvent event) {
        if (event.getTerm() > 0) {
            GROUP_UPDATE_TIME.put(event.getGroup(), event.getTerm());
            // Notifications are made of changes in cluster information
            Optional.ofNullable(WATCHERS.remove(event.getGroup()))
                    .ifPresent(watchers -> watchers.parallelStream().forEach(this::notifyWatcher));
        }
    }

    private void notifyWatcher(Watcher<HttpContext> watcher) {
        HttpContext context = watcher.getAsyncContext();
        boolean isHttp2 = context instanceof HttpContext && context.isHttp2();

        if (!isHttp2) {
            watcher.setDone(true);
        }

        boolean isFirstResponse = !HTTP2_HEADERS_SENT.getOrDefault(watcher, false);
        sendWatcherResponse(watcher, HttpResponseStatus.OK, false, isFirstResponse);
        if (isFirstResponse && isHttp2) {
            HTTP2_HEADERS_SENT.put(watcher, true);
        }

        // For HTTP/2, re-register the watcher to continue listening for future updates
        if (isHttp2 && !watcher.isDone()) {
            registryWatcher(watcher);
        }
    }

    /**
     * Send watcher response to the client.
     *
     * @param watcher     the watcher instance
     * @param nettyStatus the HTTP status code
     * @param closeStream whether to close the HTTP/2 stream (endStream=true)
     * @param sendHeaders whether to send HTTP/2 headers frame (only needed for first response)
     */
    private void sendWatcherResponse(
            Watcher<HttpContext> watcher, HttpResponseStatus nettyStatus, boolean closeStream, boolean sendHeaders) {

        HttpContext context = watcher.getAsyncContext();
        if (!(context instanceof HttpContext)) {
            logger.warn(
                    "Unsupported context type for watcher on group {}: {}",
                    watcher.getGroup(),
                    context != null ? context.getClass().getName() : "null");
            return;
        }
        ChannelHandlerContext ctx = context.getContext();
        if (!ctx.channel().isActive()) {
            HTTP2_HEADERS_SENT.remove(watcher);
            logger.warn(
                    "Netty channel is not active for watcher on group {}, cannot send response.", watcher.getGroup());
            return;
        }

        //  HTTP/1 长轮询保持原逻辑
        if (!context.isHttp2()) {
            HttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, nettyStatus, Unpooled.EMPTY_BUFFER);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

            if (!context.isKeepAlive()) {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                ctx.writeAndFlush(response);
            }
            return;
        }

        // 第一次响应，必须先发送 headers
        if (sendHeaders) {
            Http2Headers headers = new DefaultHttp2Headers().status(nettyStatus.codeAsText());
            headers.set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream; charset=utf-8");
            headers.set(HttpHeaderNames.CACHE_CONTROL, "no-cache");

            ctx.write(new DefaultHttp2HeadersFrame(headers));
        }

        String group = watcher.getGroup();
        long term = GROUP_UPDATE_TIME.getOrDefault(group, 0L);
        long now = System.currentTimeMillis();
        String type;

        // 决定事件类型
        if (sendHeaders) {
            // 第一次建立 stream 时必须给客户端一个事件
            type = "ping";
        } else if (closeStream && nettyStatus == HttpResponseStatus.NOT_MODIFIED) {
            // 超时事件
            type = "timeout";
        } else {
            // 正常集群变更事件
            type = "update";
        }

        // 构造 JSON 格式事件
        String json = String.format(
                "{\"type\":\"%s\",\"group\":\"%s\",\"term\":%d,\"timestamp\":%d}", type, group, term, now);

        // SSE 推送格式
        String sse = "data: " + json + "\n\n";

        ByteBuf content = Unpooled.copiedBuffer(sse, StandardCharsets.UTF_8);

        // 发送 DATA 帧（closeStream = true 则结束本次 stream）
        ctx.write(new DefaultHttp2DataFrame(content, closeStream));
        ctx.flush();
    }

    public void registryWatcher(Watcher<HttpContext> watcher) {
        String group = watcher.getGroup();
        Long term = GROUP_UPDATE_TIME.get(group);
        HttpContext context = watcher.getAsyncContext();
        boolean isHttp2 = context instanceof HttpContext && context.isHttp2();

        if (term == null || watcher.getTerm() >= term) {
            // For HTTP/2, must send response headers immediately, cannot delay
            if (isHttp2 && !HTTP2_HEADERS_SENT.getOrDefault(watcher, false)) {
                sendWatcherResponse(watcher, HttpResponseStatus.OK, false, true);
                HTTP2_HEADERS_SENT.put(watcher, true);
            }
            WATCHERS.computeIfAbsent(group, value -> new ConcurrentLinkedQueue<>())
                    .add(watcher);
        } else {
            notifyWatcher(watcher);
        }
    }
}
